/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package meetu.eventservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import meetu.eventservice.config.ElasticUtil;
import meetu.eventservice.model.Event;
import meetu.eventservice.model.EventTicket;
import meetu.eventservice.model.InterestGenreBehavior;
import meetu.eventservice.model.Persona;
import meetu.eventservice.model.User;
import meetu.eventservice.model.UserNotification;
import meetu.eventservice.repository.EventRepository;
import meetu.eventservice.repository.EventTicketRespository;
import meetu.eventservice.repository.UserNotificationRepository;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FuzzyQueryBuilder;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Notification;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 *
 * @author wdrdr
 */
@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventTicketRespository eventTicketRespository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private RestHighLevelClient elasticClient;

    private final String eventsIndex = "events";

    public void pushNotificationOfEvent(String eventName, String eventDetail) {
        // Create a list containing up to 100 messages.
        List<UserNotification> userNotifications = userNotificationRepository.findAll();
        List<Message> messages = new ArrayList<>();
        for (UserNotification userNotification : userNotifications) {
            messages = Arrays.asList(
                    Message.builder()
                            .setNotification(new Notification(
                                    eventName,
                                    eventDetail))
                            .setToken(userNotification.getNotificationToken())
                            .build(),
                    // ...
                    Message.builder()
                            .setNotification(new Notification("Price drop", "2% off all books"))
                            .setTopic("readers-club")
                            .build()
            );
        }
        BatchResponse response = null;
        try {
            response = FirebaseMessaging.getInstance().sendAll(messages);
        } catch (FirebaseMessagingException ex) {
            Logger.getLogger(EventService.class.getName()).log(Level.SEVERE, null, ex);
        }

// See the BatchResponse reference documentation
// for the contents of response.
        System.out.println(response.getSuccessCount() + " messages were sent successfully");
    }

    public Event createEvent(Event event) {
        Date currentDate = new Date();
        event.setCreateEventDate(currentDate);
        IndexRequest indexRequest = new IndexRequest(eventsIndex);
        String elasticEventid = null;
        Map<String, Object> pojoToMap = ElasticUtil.pojoToMap(event);
        pojoToMap.remove("organize");
        System.out.println(pojoToMap);
        indexRequest.source(pojoToMap);
        try {
            IndexResponse indexResponse = elasticClient.index(indexRequest, RequestOptions.DEFAULT);
            elasticEventid = indexResponse.getId();
            event.setElasticEventId(elasticEventid);
        } catch (IOException ex) {
            Logger.getLogger(EventService.class.getName()).log(Level.SEVERE, null, ex);
        }
        Event savedEventMongoDB = null;
        try {
            savedEventMongoDB = eventRepository.save(event);
            System.out.println("fuq !!!");
            System.out.println(savedEventMongoDB);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            savedEventMongoDB = eventRepository.save(event);
            if (savedEventMongoDB == null) {
                DeleteRequest deleteRequest = new DeleteRequest(eventsIndex);
                deleteRequest.id(elasticEventid);
                try {
                    elasticClient.delete(deleteRequest, RequestOptions.DEFAULT);
                } catch (IOException ex1) {
                    Logger.getLogger(EventService.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
        this.pushNotificationOfEvent(event.getEventName(),event.getEventDetail());
        return savedEventMongoDB;
    }

    public ResponseEntity<HashMap<String, Object>> deleteEventByElasticId(String eventId) {
        HashMap<String, Object> responseBody = new HashMap();
        Event deletedEventMongo = null;
        DeleteRequest deleteRequest = new DeleteRequest(eventsIndex, eventId);
        DeleteResponse deleteResponse = null;
        try {
            deleteResponse = elasticClient.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            Logger.getLogger(EventService.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (deleteResponse.getResult() == DocWriteResponse.Result.DELETED) {
            System.out.println(deleteResponse);
            try {
                System.out.println("---- initial delete item ----");
                eventRepository.deleteByElasticEventId(eventId);
                System.out.println("---- Deleted Event Mongo ----");
            } catch (Exception ex) {
                Logger.getLogger(EventService.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            responseBody.put("response", "remove " + eventId + " fail !");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody);
        }
        responseBody.put("response", "remove " + eventId + " successful !");
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(responseBody);
    }

    public List<Event> findEventByUsingFilter(String[] eventTags, boolean isRecently, String eventDetail, double longitude, double latitude, String areaOfEvent, int page, int contentPerPage) throws IOException {
        BoolQueryBuilder queryFilter = new BoolQueryBuilder();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        SearchRequest searchRequest = new SearchRequest(eventsIndex);
        SearchResponse searchResponse = null;

        System.out.println("--- Start Filtering ---");
        if (eventTags != null) {
            System.out.println("Event Tag Filter");
            queryFilter = filterByEventTags(queryFilter, eventTags);
        }
        if (!eventDetail.isEmpty()) {
            System.out.println("Event DetailvFilter");
            queryFilter.must(filterByEventDetail(eventDetail));
        }
        if (isRecently == true) {
            System.out.println("Recently Filter");
            searchSourceBuilder = filterByRecently(searchSourceBuilder, "createEventDate");
        }
        if (longitude != 0.0 & latitude != 0.0) {
            System.out.println("Geo Filter");
            queryFilter.filter(filterByGeolocation(latitude, longitude, areaOfEvent));
            searchSourceBuilder.sort(
                    new GeoDistanceSortBuilder("location.geopoint", latitude, longitude)
                            .unit(DistanceUnit.KILOMETERS)
                            .order(SortOrder.ASC)
            );
        }

        searchSourceBuilder.query(queryFilter);
        searchSourceBuilder
                .from(page)
                .size(contentPerPage);
        searchRequest.source(searchSourceBuilder);
        searchResponse = elasticClient.search(searchRequest, RequestOptions.DEFAULT);

        return ElasticUtil.searchHitsToList(searchResponse.getHits(), Event.class);
    }

    public List<Event> findEventByPersonalize(User user) {
        BoolQueryBuilder queryFilter = new BoolQueryBuilder();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        SearchRequest searchRequest = new SearchRequest(eventsIndex);
        SearchResponse searchResponse = null;
        Persona persona = user.getPersona();
        List<String> interestIdeaList = persona.getInterestIdea();
        List<InterestGenreBehavior> interestBehaviorList = persona.getInterestBehaviorList();
        byte numberOfRecommendationSize = 3; // จำนวน genre ที่สามารถใช้ recommend ได้เช่นมีทั้งหมด 3 ประเภท, 5 ประเภท
        int participateBoundedForWeightGenre = 2;// จำนวนขั้นต่ำของจำนวนครั้งที่เข้าร่วมกิจกรรมจึงจะนำมา weight ในการเลือก top N กิจกรรมที่เข้าร่วม
        System.out.println("Find Event By Personalize ! ");

        //Logic Recommendation
        // ขนาดที่เราจะเก็บจำนวนที่ recommendation ได้คือ numberOfRecommendationSize ถ้าเกิดจากนี้ก็จะเตะตัวที่น้อยสุดสุดออกและนำตัวใหม่เข้ามาแทนนั่นเอง
        ArrayList<InterestGenreBehavior> topNumberParticipateEvent = new ArrayList(numberOfRecommendationSize);
        int numberOfGenreThatInsideTopNListParticipate = 0;// ตัวแปรที่ใช้วัดว่าจำนวน top numberOfRecommendationSize ที่เก็บลงไปใน array ของ topNParticipate ถึงแค่ไหนแล้ว
        for (int i = 0; i < interestBehaviorList.size(); i++) {
            if (interestBehaviorList.get(i).getTotalParticipate() >= participateBoundedForWeightGenre) {
                if (numberOfGenreThatInsideTopNListParticipate < numberOfRecommendationSize) {
                    topNumberParticipateEvent.add(interestBehaviorList.get(i));
                    numberOfGenreThatInsideTopNListParticipate++;
                } else {
                    int indexOfEventThatHaveLowestScoreFromTopNParticipateList = topNumberParticipateEvent.indexOf(Collections.min(topNumberParticipateEvent));
                    if (interestBehaviorList.get(i).compareTo(topNumberParticipateEvent.get(indexOfEventThatHaveLowestScoreFromTopNParticipateList)) == 1) {
                        topNumberParticipateEvent.set(indexOfEventThatHaveLowestScoreFromTopNParticipateList, interestBehaviorList.get(i));
                    }
                }
            }
        }
        System.out.println("------ top 3 participate event (Reverse Order) -------");
        System.out.println("Total TopRank Slot : " + numberOfGenreThatInsideTopNListParticipate);
        topNumberParticipateEvent.sort(Collections.reverseOrder());
        System.out.println("top3Participate : " + topNumberParticipateEvent);

        // หลังจากที่เราได้จำนวนกิจกรรม top N ที่เข้าร่วมมาแล้ว 
        // เราจะทำการเช็คการ interestIdea ที่เขาได้ทำการ check เอาไว้ว่าสนใจในด้านใดมา weight ร่วมกับกิจกรรมที่เขาเข้าร่วม
        // ถ้าเกิด topNumberParticipate นั้นไม่มีเลยสักตัวเราก็ต้องนำตัวที่เขา check คือ ideaInterest ไปใช้แทนแต่ถ้าเป็น genre ที่สนใจตัวเดียวกันอีกก็จะยิ่ง boost เพิ่ม
        List<QueryBuilder> queryBuilderList = new ArrayList<>();
        for (int i = 0; i < numberOfGenreThatInsideTopNListParticipate; i++) {
            if (interestIdeaList.contains(topNumberParticipateEvent.get(i))) {
                // ถ้า InterestIdea ที่เลือกไว้จาก preference ตรงกับ  TopN ที่เราคัดกรองมาก็จะบวกคะแนน
                queryFilter.should().add(
                        QueryBuilders.termQuery("eventTags", topNumberParticipateEvent.get(i).getGenre().toLowerCase())
                                .boost(1.2f)
                );
            } else {
                queryFilter.should().add(
                        QueryBuilders.termQuery("eventTags", topNumberParticipateEvent.get(i).getGenre().toLowerCase()));
            }
        }

        for (int i = 0; i < numberOfGenreThatInsideTopNListParticipate; i++) {
            if (interestBehaviorList.contains(topNumberParticipateEvent.get(i))) {
                System.out.println("remove ! " + topNumberParticipateEvent.get(i));
                interestBehaviorList.remove(topNumberParticipateEvent.get(i));
            }
        }
        System.out.println("behavior filtered : " + interestBehaviorList);

        ArrayList<InterestGenreBehavior> slotForFreeSpaceOfTopNRank = new ArrayList<>(10);
        int numberSlotForFreeSpaceOfTopNRank = 0;
        for (int i = 0; i < (numberOfRecommendationSize - numberOfGenreThatInsideTopNListParticipate); i++) {
            int indexOfEventThatHaveHighestScoreFromBehaviorListAfterFilter = interestBehaviorList.indexOf(Collections.max(interestBehaviorList));
            System.out.println("Free Slot Seletected : " + interestBehaviorList.get(indexOfEventThatHaveHighestScoreFromBehaviorListAfterFilter));
            slotForFreeSpaceOfTopNRank.add(interestBehaviorList.get(indexOfEventThatHaveHighestScoreFromBehaviorListAfterFilter));
            interestBehaviorList.remove(interestBehaviorList.get(indexOfEventThatHaveHighestScoreFromBehaviorListAfterFilter));
            numberSlotForFreeSpaceOfTopNRank++;
        }

        System.out.println("--- Slot For Free Space -----");
        System.out.println("Total Free Slot : " + numberSlotForFreeSpaceOfTopNRank);
        System.out.println(slotForFreeSpaceOfTopNRank.toString());
        for (int i = 0; i < numberSlotForFreeSpaceOfTopNRank; i++) {
            if (interestIdeaList.contains(slotForFreeSpaceOfTopNRank.get(i))) {
                // ถ้า InterestIdea ที่เลือกไว้จาก preference ตรงกับ  TopN ที่เราคัดกรองมาก็จะบวกคะแนน
                queryFilter.should().add(
                        QueryBuilders.termQuery("eventTags", slotForFreeSpaceOfTopNRank.get(i).getGenre().toLowerCase())
                                .boost(1.2f)
                );
            } else {
                queryFilter.should().add(QueryBuilders.termQuery("eventTags", slotForFreeSpaceOfTopNRank.get(i).getGenre().toLowerCase()));
            }
        }
        searchSourceBuilder.query();
        searchRequest.source(searchSourceBuilder);
        try {
            searchResponse = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            Logger.getLogger(EventService.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ElasticUtil.searchHitsToList(searchResponse.getHits(), Event.class);
    }

    public BoolQueryBuilder filterByEventTags(BoolQueryBuilder queryFilter, String eventTags[]) {
        queryFilter.filter(QueryBuilders.termsQuery("eventTags", eventTags));
        return queryFilter;
    }

    public QueryStringQueryBuilder filterByEventDetail(String eventDetail) {
        QueryStringQueryBuilder alreadyFilterByEventDetail = QueryBuilders.queryStringQuery(eventDetail + "~")
                .field("eventName").boost(2.0f)
                .field("eventDetail").boost(2.0f)
                .field("location.*").boost(5.0f)
                .fuzzyTranspositions(true);
        return alreadyFilterByEventDetail;
    }

    public GeoDistanceQueryBuilder filterByGeolocation(double latitude, double longitude, String areaOfEvent) {
        return QueryBuilders.geoDistanceQuery("location.geopoint")
                .point(latitude, longitude)
                .distance(areaOfEvent);
    }

    public SearchSourceBuilder filterByRecently(SearchSourceBuilder searchSourceBuilder, String sortedField) throws IOException {
        searchSourceBuilder.sort(sortedField, SortOrder.DESC);
        return searchSourceBuilder;
    }

    public ResponseEntity findEventByEventId(String eventId) {
        return ResponseEntity.status(HttpStatus.OK).body(eventRepository.findByElasticEventId(eventId));
    }

    public ResponseEntity findEventByElasticId(String elasticEventId) {
        GetResponse getResponse = null;
        GetRequest getRequest = new GetRequest(eventsIndex, elasticEventId);
        HashMap<String, Object> responseBody = new HashMap<>();
        try {
            getResponse = elasticClient.get(getRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            Logger.getLogger(EventService.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("-------- Response -----------");
        System.out.println(getResponse);
        System.out.println(getResponse.isExists());
        if (getResponse.isExists()) {
            ObjectMapper mapper = new ObjectMapper();
            Event convertValue = mapper.convertValue(getResponse.getSource(), Event.class);
            return ResponseEntity.status(HttpStatus.OK).body(convertValue);
        }
        responseBody.put("response", "Not found EventID: " + elasticEventId + " !");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody);
    }

    public EventTicket userJoinEvent(EventTicket userJoinEvent) {
        userJoinEvent.setIsParticipate(true);
        userJoinEvent.setParticipateDate(new Timestamp(System.currentTimeMillis()));
        return eventTicketRespository.save(userJoinEvent);
    }

    public EventTicket userReserveTicket(EventTicket userReserveTicket) {
        qrCodeService.getQRCodeImage(eventsIndex, 0, 0);
        return null;
    }

    public UserNotification saveuserNotification(UserNotification userNotification) {
        return userNotificationRepository.save(userNotification);
    }

}
