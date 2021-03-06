/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package meetu.userservice.model;

/**
 *
 * @author wdrdr
 */
public class InterestGenreBehavior implements Comparable<InterestGenreBehavior> {

    private String genre;

    private int totalView;

    private int totalParticipate;

    private int absent;
    
    public InterestGenreBehavior(){
        
    }

    public InterestGenreBehavior(String genre, int totalView, int totalParticipate, int absent) {
        this.genre = genre;
        this.totalView = totalView;
        this.totalParticipate = totalParticipate;
        this.absent = absent;
    }
    
    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getTotalView() {
        return totalView;
    }

    public void setTotalView(int totalView) {
        this.totalView = totalView;
    }

    public int getTotalParticipate() {
        return totalParticipate;
    }

    public void setTotalParticipate(int totalParticipate) {
        this.totalParticipate = totalParticipate;
    }

    public int getAbsent() {
        return absent;
    }

    public void setAbsent(byte absent) {
        this.absent = absent;
    }

    @Override
    public String toString() {
        return "InterestBehavior{" + "genre=" + genre + ", totalView=" + totalView + ", totalParticipate=" + totalParticipate + ", absent=" + absent + '}';
    }

    @Override
    public int compareTo(InterestGenreBehavior o) {
        if (this.totalParticipate < o.getTotalParticipate()) {
            return -1;
        }
        if (this.totalParticipate == o.getTotalParticipate()) {
            if (this.totalView > o.totalView) {
                return 1;
            } else if (this.totalView == o.totalView) {
                return 0;
            } else {
                return -1;
            }
        }
        if (this.totalParticipate > o.getTotalParticipate()) {
            return 1;
        }
        return -1;
    }

}
