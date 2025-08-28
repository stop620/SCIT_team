document.addEventListener('DOMContentLoaded',()=>{
    // 그래프 캔버스 선언 및 가져오기
    var studyStats = document.getElementById('study-stats');
    var studyTracker = document.getElementById('study-tracker');
    var gradeTrend = document.getElementById('grade-trend');
    var etc = document.getElementById('etc');

    const gradeTrendData = {
        labels : ['0822','0823','0824','0825','0826','0827','0828'],
        datasets : [{
            backgroundColor : 'rgba(75, 192, 192, 1)',
            borderColor : 'rgba(75, 192, 192, 1)',
            label : '성적 통계',
            fill : false,
            data : [
                5,4,6,7,8,9,7
            ]
        }],
        options : {
            maintainAspectRation : true,
            title : {
                text: '성적 추이'
            },
            scales : {
                yAxes : [{
                    scaleLabel : {
                        display : true,
                        labelString : '정답 수'
                    }
                }]
            }
        }
    };

    const studyStatsChart = new Chart(studyStats,{
    type:'doughnut',
    data: {
    labels: ['January', 'February', 'March', 'April', 'May'],
    datasets: [
            {
                data: [50, 60, 70, 180, 190],
            },
        ],
    }
    });

    const studyTrackerChart = new Chart(studyTracker,{
        type:'doughnut',
        data: {
        labels: ['January', 'February', 'March', 'April', 'May'],
        datasets: [
                {
                    data: [50, 60, 70, 180, 190],
                },
            ],
        }
    });

    const gradeTrendChart = new Chart(gradeTrend,{
        type : 'line',
        data : {
            labels : ['0822','0823','0824','0825','0826','0827','0828'],
            datasets : [{
                backgroundColor : 'rgba(75, 192, 192, 1)',
                borderColor : 'rgba(75, 192, 192, 1)',
                label : '성적 통계',
                fill : false,
                data : [
                    5,4,6,7,8,9,7
                ]
            }],
            options : {
                maintainAspectRation : true,
                title : {
                    text: '성적 추이'
                },
                scales : {
                    yAxes : [{
                        scaleLabel : {
                            display : true,
                            labelString : '정답 수'
                        }
                    }]
                }
            }
        }
    });

    const etcChart = new Chart(etc,{
        //그래프가 필요하면 추가 하기
    });

});

