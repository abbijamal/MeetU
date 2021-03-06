/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package meetu.userservice.repository;

import java.util.List;
import java.util.Optional;
import meetu.userservice.model.Admin;
import meetu.userservice.model.User;
import meetu.userservice.model.UserBadge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

/**
 *
 * @author wdrdr
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    public List<User> findByFirstName(String firstName);
    
    public List<User> findByEmailLike(String emailKeyWord);
  
    public User findByUsername(String username);
    
    public List<Admin> findByEmailIsIn(List<String> emailList );
    
    public User findByUid(String uid);

    public Optional<User> findByBadgeListBadgeIdEquals(String badgeId);

    public User findByEmailEquals(String email);
    
    
}
