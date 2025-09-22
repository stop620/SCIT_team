
document.addEventListener('DOMContentLoaded', function() {
    console.log("Navigation script loaded");

    const quizCard1 = document.getElementById("quizCard1");
    const quizCard2 = document.getElementById("quizCard2");
    const quizCard3 = document.getElementById("quizCard3");

    quizCard1.onclick = function(){
        console.log(this);
        quizOpen(1);
    };
    quizCard2.onclick = function(){
        console.log(this);
        quizOpen(2);
    };
    quizCard3.onclick = function(){
        console.log(this);
        quizOpen(3);
    };
});

function quizOpen(level){
    location.href = '/mochilearn/page/quizCardPage' + '?level=' + level;
};