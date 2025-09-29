document.addEventListener('DOMContentLoaded', ()=>{
    var resultChart = document.querySelector('.resultChart');

    const resultConfig = {
        type: 'doughnut',
        data: {
            datasets:[{
                data : [7,3]
            }],
            labels : ['정답','오답']
        }
    };
    var newChart = new Chart(resultChart,resultConfig);
    console.log('chart loaded');
});
