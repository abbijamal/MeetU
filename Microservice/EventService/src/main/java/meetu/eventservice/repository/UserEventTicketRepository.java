/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package meetu.eventservice.repository;

import java.util.List;
import meetu.eventservice.model.EventTicket;
import meetu.eventservice.model.UserEventTicket;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 *
 * @author Test
 */
public interface UserEventTicketRepository  extends MongoRepository<UserEventTicket, String> {
    
    public UserEventTicket findByTicketId(String ticketId);
    
    public List<UserEventTicket> findByUid(String uid);

    public UserEventTicket findByUidAndElasticEventIdAndIsParticipate(String uid, String elasticEventId, boolean isParticipate);
    
    
}
