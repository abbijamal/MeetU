<template>
  <div>
    <rating-event></rating-event>
  </div>
</template>

<script>
import ratingEvent from "~/components/ratingEvent";

export default {
  components: {
    ratingEvent
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
</style>
