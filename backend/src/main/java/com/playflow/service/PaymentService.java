package com.playflow.service;

import com.playflow.dto.ApiDtos.*;
import com.playflow.repository.*;
import com.playflow.entity.Entities.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.util.*;

@Service public class PaymentService {
    private final UserRepository users; private final WalletRepository wallets; private final PaymentRepository payments; private final JourneyRepository journeys; private final KafkaTemplate<String,PaymentEvent> events;
    public PaymentService(UserRepository users,WalletRepository wallets,PaymentRepository payments,JourneyRepository journeys,KafkaTemplate<String,PaymentEvent> events){this.users=users;this.wallets=wallets;this.payments=payments;this.journeys=journeys;this.events=events;}
    @Transactional public PaymentView transfer(Long senderId,TransferRequest request,String key){
        if(key==null||key.isBlank()||key.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A valid Idempotency-Key header is required");
        var prior=payments.findByIdempotencyKey(key);if(prior.isPresent()){Payment p=prior.get();String description=request.description()==null||request.description().isBlank()?"Money transfer":request.description().trim();if(!p.senderId.equals(senderId)||!Objects.equals(p.receiverId,request.receiverId())||request.amount()==null||p.amount.compareTo(request.amount())!=0||!p.description.equals(description))throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key was already used for a different payment");return view(p);}
        if(request.receiverId()==null||request.receiverId().equals(senderId))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose another user as recipient");
        BigDecimal amount=validAmount(request.amount()); if(!users.existsById(request.receiverId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Recipient not found");
        Long first=Math.min(senderId,request.receiverId()),second=Math.max(senderId,request.receiverId());
        Wallet one=wallets.lockByUserId(first).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Wallet not found"));
        Wallet two=wallets.lockByUserId(second).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Wallet not found"));
        Wallet from=senderId.equals(first)?one:two,to=senderId.equals(first)?two:one;
        String description=request.description()==null||request.description().isBlank()?"Money transfer":request.description().trim();
        if(from.balance.compareTo(amount)<0){Payment failed=new Payment();failed.transactionId=UUID.randomUUID().toString();failed.senderId=senderId;failed.receiverId=request.receiverId();failed.amount=amount;failed.status="FAILED";failed.description=description;failed.category=classify(description);failed.idempotencyKey=key;failed=payments.save(failed);event(failed,"REQUESTED","Transfer request received");event(failed,"VALIDATED","Recipient verified; wallet balance check failed");event(failed,"FAILED","Insufficient wallet balance; no money was moved");publish(failed);return view(failed);}
        from.balance=from.balance.subtract(amount);to.balance=to.balance.add(amount);
        Payment p=new Payment();p.transactionId=UUID.randomUUID().toString();p.senderId=senderId;p.receiverId=request.receiverId();p.amount=amount;p.status="PROCESSING";p.description=description;p.category=classify(p.description);p.idempotencyKey=key;
        p=payments.save(p); event(p,"REQUESTED","Transfer request received");event(p,"VALIDATED","Recipient and wallet balances validated");event(p,"PROCESSING","Wallet balances updated");p.status="SUCCESS";p=payments.save(p);event(p,"SUCCESS","Money sent successfully");
        publish(p);return view(p);
    }
    @Transactional public WalletView changeBalance(Long userId,BigDecimal value,boolean deposit){
        BigDecimal amount=validAmount(value);Wallet w=wallets.lockByUserId(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Wallet not found"));
        if(!deposit&&w.balance.compareTo(amount)<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Insufficient wallet balance");
        w.balance=deposit?w.balance.add(amount):w.balance.subtract(amount);
        Payment p=new Payment();p.transactionId=UUID.randomUUID().toString();p.senderId=deposit?userId:userId;p.receiverId=userId;p.amount=amount;p.status="SUCCESS";p.description=deposit?"Wallet deposit":"Wallet withdrawal";p.category="Wallet";payments.save(p);event(p,"REQUESTED",p.description+" requested");event(p,"VALIDATED","Wallet balance validated");event(p,"PROCESSING","Wallet balance updated");event(p,"SUCCESS",p.description+" completed");
        publish(p);return new WalletView(userId,w.balance,w.currency);
    }
    private BigDecimal validAmount(BigDecimal amount){if(amount==null||amount.compareTo(BigDecimal.ZERO)<=0||amount.scale()>2)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Amount must be positive and have at most 2 decimal places");return amount;}
    private void event(Payment p,String stage,String note){journeys.save(new JourneyEvent(p.transactionId,stage,note));}
    private void publish(Payment p){Runnable send=()->{try{events.send("payflow.payment.events",p.transactionId,new PaymentEvent(p.transactionId,p.senderId,p.receiverId,p.amount,p.status,p.description,p.createdAt)).whenComplete((result,error)->{if(error!=null)System.err.println("Payment event publish failed for "+p.transactionId+": "+error.getMessage());});}catch(RuntimeException error){System.err.println("Payment event publish failed for "+p.transactionId+": "+error.getMessage());}};if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){send.run();}});else send.run();}
    public String classify(String d){String s=d==null?"":d.toLowerCase(Locale.ROOT);if(has(s,"dinner","lunch","restaurant","food","cafe","grocery","groceries","भोजन"))return "Food & Dining";if(has(s,"movie","concert","game","music","netflix"))return "Entertainment";if(has(s,"flight","hotel","travel","cab","uber","train"))return "Travel";if(has(s,"electricity","water bill","internet","recharge","utility"))return "Utilities";if(has(s,"bill","rent","insurance","emi"))return "Bills";if(has(s,"shop","clothes","fashion","amazon","purchase"))return "Shopping";return "Transfers";}
    private boolean has(String s,String...words){return Arrays.stream(words).anyMatch(s::contains);}
    public PaymentView view(Payment p){return new PaymentView(p.transactionId,p.senderId,p.receiverId,p.amount,p.status,p.description,p.category,p.createdAt);}
    public Payment get(String id,Long userId){Payment p=payments.findByTransactionId(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Transaction not found"));if(!p.senderId.equals(userId)&&!p.receiverId.equals(userId))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Transaction not found");return p;}
    public List<PaymentView> transactions(Long userId){return payments.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId,userId).stream().map(this::view).toList();}
    public List<JourneyView> journey(String id,Long userId){get(id,userId);return journeys.findByTransactionIdOrderByOccurredAtAsc(id).stream().map(e->new JourneyView(e.stage,e.note,e.occurredAt)).toList();}
    private boolean has(Payment p,Long userId){return p.senderId.equals(userId);}
}
