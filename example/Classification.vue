<template>
  <div class="hello">
    <h1>{{ msg }}</h1>
    <p><mytable></mytable></p>
  </div>
</template>
 
<script>
import table from '@/components/table'
export default {
  name: 'dt',
  data () {
    return {
      msg: '欢迎来到分类！'
    }
  },
  components:{
      "mytable": table 
  }
}
</script>