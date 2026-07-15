<template>
  <div>
  <p>
    <h1><button type="button" class="btn btn-default btn-primary" @click="show()">载入数据</button></h1>
    <h1><div id="show" style="width: 600px;height:400px;"></div></h1>
 </p>
 <p>  
    <form>
    <label>input K</label>
    <input
      v-model="k"
      id="new-todo"
      placeholder="3"
    >
    <button @click="clustering()">聚类分析</button>
  </form>
    <h1><div id="result"  style="width: 1000px;height:600px;"></div></h1>
  </p>
    
    
  </div>

</template>

<script>
import axios from 'axios';
export default {
  name: 'apriori',
  data () {
    return {
        aa: 'hello',
        k: 3,
        resources: '',
        days: [],
        hours: [],
        dataHot: []
    }
  },
  created() {
    this.getMessage();
  },
  methods: {
    show(){
        var myChart = this.$echarts.init(document.getElementById('show'));
        myChart.setOption({
            series : [
                {
                    name: '访问来源',
                    type: 'pie',    // 设置图表类型为饼图
                    radius: '55%',  // 饼图的半径，外半径为可视区尺寸（容器高宽中较小一项）的 55% 长度。
                    data:[          // 数据数组，name 为数据项名称，value 为数据项值
                        {value:235, name:'视频广告'},
                        {value:274, name:'联盟广告'},
                        {value:310, name:'邮件营销'},
                        {value:335, name:'直接访问'},
                        {value:400, name:'搜索引擎'}
                    ]
                }
            ]
        })
    },
    clustering() {
          const path = `http://localhost:5000/clustering`;
          var dict = {"K":this.k};
          console.log(dict)
          axios.post(path,dict).then((res) =>{
                setTimeout(() => {
                  console.log(res.data)
                  this.result();
                },5000);              
              
              
              })
              .catch((error) => {
              // eslint-disable-next-line
              console.error(error);

                });
    },
    getMessage() {
          const url = 'http://127.0.0.1:5000/heatmap';
          axios.get(url).then((res) => {
            this.resources = res.data;
            this.hours = this.resources.hours;
            this.days = this.resources.days;
            this.dataHot = this.resources.dataHot.map(function (item) {
                return [item[1], item[0], item[2] || '-'];
            });
          })
          .catch((error) => {
            console.error(error);
          });

    },
    result(){
        var myChart = this.$echarts.init(document.getElementById('result'));
        console.log(this.days)
        console.log(this.dataHot)
        var option = {
            tooltip: {
                position: 'top'
            },
            grid: {
                height: '50%',
                top: '10%'
            },
            xAxis: {
                type: 'category',
                data: this.hours,
                splitArea: {
                    show: true
                }
            },
            yAxis: {
                type: 'category',
                data: this.days,
                splitArea: {
                    show: true
                }
            },
            visualMap: {
                min: 0,
                max: 10,
                calculable: true,
                orient: 'horizontal',
                left: 'center',
                bottom: '15%'
            },
            series: [{
                name: 'Punch Card',
                type: 'heatmap',
                data: this.dataHot,
                label: {
                    show: true
                },
                emphasis: {
                    itemStyle: {
                        shadowBlur: 10,
                        shadowColor: 'rgba(0, 0, 0, 0.5)'
                    }
                }
            }]
        };
        
        myChart.setOption(option);
    }


  }
}

</script>

