<template>
  <!-- <nuxt-link :to="`/event/${event.elasticEventId}`"> -->
  <v-card>
    <v-img :src="event.eventPictureCover" height="200px"></v-img>
    <v-container fill-height fluid pa-2>
      <v-layout fill-height>
        <v-flex xs12 align-end flexbox>
          <span
            class="textName"
            v-text="event.eventName.length > 20 ? event.eventName.substr(0,40)+'...' :event.eventName "
          ></span>
        </v-flex>
      </v-layout>
    </v-container>
    <!-- <v-card-actions>
        <h3 class="#AEAEAE--text">{{event.eventName}}</h3>
    </v-card-actions>-->
    <!-- <v-flex v-if="isOwner" class="text-xs-right">
      <v-btn fab dark small color="#341646" @click="$emit('editEvent', event)">
        <v-icon color="white" medium>poll</v-icon>
      </v-btn>
      <v-btn fab dark small color="red" @click="$emit('deleteEvent', event)">
        <v-icon color="#fff" medium>delete</v-icon>
      </v-btn>
    </v-flex>-->
    <v-slide-y-transition>
      <v-card-text>
        <v-layout row wrap>
          <v-layout>
            <v-flex class="eventMonth">
              <v-icon size="20">today</v-icon>
              {{formatDateForReadable(event.eventStartDate)}}
            </v-flex>
            <v-flex xs7 class="eventMonth">
              <v-icon size="20">alarm</v-icon>
              {{formatAMPM(event.eventStartDate)}}
            </v-flex>
          </v-layout>
        </v-layout>
        <!-- <span
            v-text="location.detail.length > 20 ? location.detail.substr(0,40)+'...' :location.detail"
        ></span>-->
        <v-layout row wrap>
          <v-flex>
            <div class="b">
              <v-icon size="20">room</v-icon>
              {{ location.detail===''?'Not have yet...' :location.detail }}
            </div>
          </v-flex>
        </v-layout>
        <v-layout row wrap>
          <v-flex v-if="isOwner" xs6>
            <v-btn block dark small color="#341646" @click="$emit('editEvent', event)">
              <v-icon color="white" medium>poll</v-icon>View Statistics
            </v-btn>
          </v-flex>
          <v-flex xs6 v-if="isOwner">
            <v-btn block small color="grey lighten-2" @click="$emit('deleteEvent', event)">
              <v-icon color="grey darken-1" medium>delete</v-icon>
              <div class="b">Delete Event</div>
            </v-btn>
          </v-flex>
        </v-layout>

        <!-- {{event.location.province?event.location.province:'Thailand'}} -->
      </v-card-text>
    </v-slide-y-transition>
  </v-card>
  <!-- </nuxt-link> -->
</template>
<script>
import Swal from "sweetalert2";
export default {
  name: "EventCard",
  components: {},
  data() {
    return {
      isEditing: true
    };
  },

  props: {
    event: Object,
    isOwner: Boolean,
    isAdmin: Boolean,
    location: {
      type: Object,
      default: {
        detail: "Not have information yet..."
      }
    },
    badge: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  methods: {
    formatDateForReadable: function(formatDate) {
      const months = [
        "JAN",
        "FEB",
        "MAR",
        "APR",
        "MAY",
        "JUN",
        "JUL",
        "AUG",
        "SEP",
        "OCT",
        "NOV",
        "DEC"
      ];
      let date = new Date(formatDate);
      formatDate =
        date.getDate() +
        " " +
        months[date.getMonth()] +
        " " +
        date.getFullYear();
      console.log(formatDate);
      return formatDate;
    },
    formatAMPM: function(AMPM) {
      let date = new Date(AMPM);
      const options = {
        hour: "2-digit",
        minute: "2-digit"
      };
      let time = date.toLocaleTimeString("en-US", options);
      return time;
    },
    getTime: function(time) {
      let date = new Date(time);

      time = date.getHours() + ":" + date.getMinutes() + " ";
      return time;
    },

    activateInEditMode() {
      this.isEditing = false;
    },
    deActivateInEditMode() {
      this.isEditing = true;
    }
  }
};
</script>


<style lang="css">
.v-content {
  max-width: 100%;
  background-color: #eeeeee;
  font-family: Roboto;
  /* background-image: url(../assets/bg.png) !important; */
  /* background-repeat: repeat; */
  /* background-attachment: fixed;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  background: transparent; */
}

/* 
.imgBlur{
   filter: blur(1px) ;
  -webkit-filter: blur(1px);
} */

.textName {
  color: #fff;
  background-color: rgb(102, 36, 93); /* Fallback color */
  background-color: rgba(40, 25, 58, 0.6); /* Black w/opacity/see-through */
  font-weight: bold;
  /* border: 3px solid #341646; */
  position: absolute;
  top: 10%;
  left: 50%;
  transform: translate(-50%, -50%);
  /* z-index: 2; */
  width: 100%;
  padding: 20px;
  text-align: left;
}
.eventDate {
  color: #341646;
}
.eventMonth {
  font-size: 15px;
}
div.b {
  white-space: nowrap;
  width: 250px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.button {
  color: darkgray;
}

.size {
  size: 0px;
}
</style>