let gradeChart;
let levelChart;
let gradeData = {
    labels: [],
    scores: []
};
let levelData = [0, 0, 0];

document.addEventListener('DOMContentLoaded',()=>{
    // // 그래프 캔버스 선언 및 가져오기
    // var studyStats = document.getElementById('study-stats');
    let studyTracker = document.getElementById('study-tracker');
    let gradeTrend = document.getElementById('grade-trend');
    let etc = document.getElementById('etc');


    fetch('/mochilearn/api/user/session', {
        method: 'GET'
    })
        .then(response => response.json())
        .then(data => {
            
            if(data.loggedIn) {
                sessionStorage.setItem("member", JSON.stringify(data.member));
                console.log(data.member);
            } else {
                console.log('로그인 안됨')
            }
            const member = sessionStorage.getItem("member");
            const id = JSON.parse(member).id;

            fetchMemberWriteCards(id);
            fetchMemberLikeCards(id);
            fetchMemberQuizLogs(id);
        });



    // const gradeTrendData = {
    //     labels : ['0822','0823','0824','0825','0826','0827','0828'],
    //     datasets : [{
    //         backgroundColor : 'rgba(75, 192, 192, 1)',
    //         borderColor : 'rgba(75, 192, 192, 1)',
    //         label : '성적 통계',
    //         fill : false,
    //         data : [
    //             5,4,6,7,8,9,7
    //         ]
    //     }]
    // };

    // const studyStatsChart = new Chart(studyStats,{
    //     type:'doughnut',
    //     data: {
    //     labels: ['January', 'February', 'March', 'April', 'May'],
    //     datasets: [{
    //                     data: [50, 60, 70, 180, 190],
    //                 },
    //             ],
    //         }
    // });

    const studyTrackerChart = new Chart(studyTracker,{
        type:'doughnut',
        data: {
        labels: ['January', 'February', 'March', 'April', 'May'],
        datasets: [{
                    data: [50, 60, 70, 180, 190],
                    },
                ],
            }
    });

    gradeChart = new Chart(gradeTrend,{
            type : 'line',
            data : {
                labels : [],
                datasets : [{
                    backgroundColor : 'rgba(75, 192, 192, 1)',
                    borderColor : 'rgba(75, 192, 192, 1)',
                    label : '성적 통계',
                    fill : false,
                    data : []
                }]
            },
            options : {
                maintainAspectRatio : false,
                //responsive: false,
                title : {
                    display: true,
                    text: '성적 추이'
                },
                scales : {
                    y: {
                        min : 0,
                        max : 5,
                        ticks: {
                            stepSize :1
                        }
                    }
                }
            }
        });

    levelChart = new Chart(etc, {
        type : 'bar',
        data : {
            labels : ['초','중','고'],
            datasets : [{
                label : '응시 횟수',
                fill : false,
                data : [0,0,0]
            }],
        },
        options : {
            maintainAspectRatio : false,
            //responsive: false,
            plugins: {
                title : {
                    display: true,
                    text : '응시 횟수'
                }
            },
            scales : {
                y: {
                    min: 0,      // 최소값 0
                    max: 10,     // 최대값 10
                    ticks: {
                        stepSize: 2
                    },
                    title: {
                        display: true,
                        text: '응시 횟수'
                    }
                }
            }
        }
    });

    // Calender Script
    class DynamicCalendar {
            constructor() {
                this.currentDate = new Date();
                this.today = new Date();
                this.selectedDate = null;
                this.attendanceData = {}; // 출석 데이터 저장소
                this.monthNames = [
                    '1월', '2월', '3월', '4월', '5월', '6월',
                    '7월', '8월', '9월', '10월', '11월', '12월'
                ];
                this.dayNames = ['일', '월', '화', '수', '목', '금', '토'];
                this.init();
            }

            init() {
                this.bindEvents();
                this.render();
                this.updateTodayInfo();
                // 샘플 출석 데이터 로드 (실제로는 DB에서 가져올 데이터)
                this.loadSampleAttendanceData();
            }

            // DB에서 출석 데이터를 가져오는 메서드
            async loadAttendanceData(year, month) {
                // 예상 반환 형식:
                // {
                //   "2025-08-15": "attended",    // 출석
                //   "2025-08-20": "partial",     // 부분출석  
                //   "2025-08-25": "absent"       // 결석
                // }
                
                console.log('DB에서 출석 데이터를 로드합니다:', year, month);
                // 실제 구현 시에는 이 부분을 API 호출로 대체
                return {};
            }

            // 출석 데이터 설정 메서드
            setAttendanceData(attendanceData) {
                this.attendanceData = attendanceData;
                this.render();
            }

            // 특정 날짜의 출석 상태 추가/업데이트
            updateAttendanceStatus(date, status) {
                const dateKey = this.formatDateKey(date);
                this.attendanceData[dateKey] = status;
                this.render();
            }

            // 샘플 데이터 (테스트용 - 실제로는 DB에서 가져올 데이터)
            loadSampleAttendanceData() {
                const currentYear = this.currentDate.getFullYear();
                const currentMonth = this.currentDate.getMonth();
                
                // 현재 월의 샘플 출석 데이터
                this.attendanceData = {
                    [`${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-05`]: 'attended',
                    [`${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-08`]: 'attended',
                    [`${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-12`]: 'partial',
                    [`${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-15`]: 'attended',
                    [`${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-18`]: 'absent',
                    [`${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-22`]: 'attended',
                    [`${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-25`]: 'partial',
                    [`${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-28`]: 'attended'
                };
            }

            bindEvents() {
                document.getElementById('prevBtn').addEventListener('click', () => {
                    this.previousMonth();
                });
                
                document.getElementById('nextBtn').addEventListener('click', () => {
                    this.nextMonth();
                });
            }

            previousMonth() {
                this.currentDate.setMonth(this.currentDate.getMonth() - 1);
                this.render();
            }

            nextMonth() {
                this.currentDate.setMonth(this.currentDate.getMonth() + 1);
                this.render();
            }

            render() {
                this.renderHeader();
                this.renderCalendar();
            }

            renderHeader() {
                const monthElement = document.getElementById('currentMonth');
                monthElement.textContent = `${this.currentDate.getFullYear()}년 ${this.monthNames[this.currentDate.getMonth()]}`;
            }

            renderCalendar() {
                const grid = document.getElementById('calendarGrid');
                grid.innerHTML = '';

                // 요일 헤더 생성
                this.dayNames.forEach(day => {
                    const dayHeader = document.createElement('div');
                    dayHeader.className = 'day-header';
                    dayHeader.textContent = day;
                    grid.appendChild(dayHeader);
                });

                // 현재 월의 첫 번째 날과 마지막 날 계산
                const year = this.currentDate.getFullYear();
                const month = this.currentDate.getMonth();
                const firstDay = new Date(year, month, 1);
                const lastDay = new Date(year, month + 1, 0);
                
                // 첫 번째 주의 빈 칸 계산
                const startDate = new Date(firstDay);
                startDate.setDate(startDate.getDate() - firstDay.getDay());
                
                // 6주간의 날짜 생성 (42일)
                for (let i = 0; i < 42; i++) {
                    const cellDate = new Date(startDate);
                    cellDate.setDate(startDate.getDate() + i);
                    
                    const dayCell = this.createDayCell(cellDate, month);
                    grid.appendChild(dayCell);
                }
            }

            createDayCell(date, currentMonth) {
                const cell = document.createElement('div');
                cell.className = 'day-cell';
                cell.textContent = date.getDate();
                
                // 다른 월의 날짜 표시
                if (date.getMonth() !== currentMonth) {
                    cell.classList.add('other-month');
                }
                
                // 출석 상태 확인 및 적용
                const attendanceStatus = this.getAttendanceStatus(date);
                if (attendanceStatus && date.getMonth() === currentMonth) {
                    cell.classList.add(attendanceStatus);
                }
                
                // 오늘 날짜 표시 (출석 상태보다 우선)
                if (this.isSameDate(date, this.today)) {
                    cell.classList.add('today');
                }
                
                // 선택된 날짜 표시
                if (this.selectedDate && this.isSameDate(date, this.selectedDate)) {
                    cell.classList.add('selected');
                }
                
                // 클릭 이벤트 추가
                cell.addEventListener('click', () => {
                    this.selectDate(date);
                });
                
                return cell;
            }

            // 특정 날짜의 출석 상태 반환
            getAttendanceStatus(date) {
                const dateKey = this.formatDateKey(date);
                return this.attendanceData[dateKey];
            }

            // 날짜를 키 형식으로 변환
            formatDateKey(date) {
                const year = date.getFullYear();
                const month = String(date.getMonth() + 1).padStart(2, '0');
                const day = String(date.getDate()).padStart(2, '0');
                return `${year}-${month}-${day}`;
            }

            selectDate(date) {
                this.selectedDate = new Date(date);
                this.render();
                this.updateSelectedInfo();
            }

            isSameDate(date1, date2) {
                return date1.getFullYear() === date2.getFullYear() &&
                       date1.getMonth() === date2.getMonth() &&
                       date1.getDate() === date2.getDate();
            }

            updateTodayInfo() {
                const todayElement = document.getElementById('todayInfo');
                const todayStr = `오늘: ${this.today.getFullYear()}년 ${this.today.getMonth() + 1}월 ${this.today.getDate()}일 (${this.dayNames[this.today.getDay()]})`;
                todayElement.textContent = todayStr;
            }

            updateSelectedInfo() {
                if (this.selectedDate) {
                    const todayElement = document.getElementById('todayInfo');
                    const selectedStr = `선택된 날짜: ${this.selectedDate.getFullYear()}년 ${this.selectedDate.getMonth() + 1}월 ${this.selectedDate.getDate()}일 (${this.dayNames[this.selectedDate.getDay()]})`;
                    todayElement.textContent = selectedStr;
                }
            }

            // 특정 날짜로 이동하는 메서드
            goToDate(year, month, day) {
                this.currentDate = new Date(year, month, day || 1);
                this.render();
            }

            // 오늘로 돌아가는 메서드
            goToToday() {
                this.currentDate = new Date(this.today);
                this.render();
            }
        }
        
        const calendar = new DynamicCalendar();
        
        // 전역 함수로 달력 인스턴스 접근 가능하게 함
        window.calendar = calendar;
        
        // 동료가 DB에서 출석 데이터를 가져와서 설정할 수 있는 전역 함수들
        window.setAttendanceData = (data) => calendar.setAttendanceData(data);
        window.updateAttendanceStatus = (date, status) => calendar.updateAttendanceStatus(date, status);
        
        // 키보드 단축키 추가
        document.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowLeft') {
                calendar.previousMonth();
            } else if (e.key === 'ArrowRight') {
                calendar.nextMonth();
            } else if (e.key === 'Home') {
                calendar.goToToday();
            }

    });
});

function updateCharts() {
    console.log('updateCharts');

    gradeChart.data.labels = gradeData.labels;
    gradeChart.data.datasets[0].data = gradeData.scores;
    levelChart.data.datasets[0].data = levelData;

    gradeChart.update();
    levelChart.update();
}

function fetchMemberWriteCards(id) {

    console.log(id);
    fetch(`/mochilearn/api/member/mycard?id=${id}`, {
        method: 'GET'
    })
        .then(response => response.json())
        .then(data => {
            console.log(data);
            renderCards(data, 0);
        });
}

function fetchMemberLikeCards(id) {

    console.log(id);
    fetch(`/mochilearn/api/member/likecard?id=${id}`, {
        method: 'GET'
    })
        .then(response => response.json())
        .then(data => {
            console.log(data);
            renderCards(data, 1);
        });
}

function fetchMemberQuizLogs(id) {

    console.log(id);
    fetch(`/mochilearn/api/member/quizlogs?id=${id}`, {
        method: 'GET'
    })
        .then(response => response.json())
        .then(data => {
            const dailyScores = {};

            data.forEach((e, index) => {

                // 응시 난이도 데이터 추가
                if(e.level == 1) {
                    levelData[0]++;
                } else if(e.level == 2) {
                    levelData[1]++;
                } else {
                    levelData[2]++;
                }

                // 성적 통계 데이터 추가
                const date = new Date(e.attemptDate).toISOString().split('T')[0];
                const score = e.score;

                // 해당 날짜에 데이터가 없으면 초기화
                if (!dailyScores[date]) {
                    dailyScores[date] = { total: 0, count: 0 };
                }
                // 점수 합계와 횟수 누적
                dailyScores[date].total += score;
                dailyScores[date].count++;
            });

            console.log('날짜별 점수 데이터:', dailyScores);

            // dailyScores 객체를 순회하며 평균 계산 및 gradeData에 저장
            // 날짜를 오름차순으로 정렬하여 차트에 반영
            const sortedDates = Object.keys(dailyScores).sort();

            // 기존 데이터 초기화
            gradeData.labels = [];
            gradeData.scores = [];

            sortedDates.forEach(date => {
                const averageScore = (dailyScores[date].total / dailyScores[date].count);
                gradeData.labels.push(date); // 날짜를 라벨로 추가
                gradeData.scores.push(averageScore); // 평균 점수를 데이터로 추가
            });

            console.log('최종 gradeData:', gradeData);
            console.log('최종 levelData:', levelData);

            renderQuizLogs(data);
            updateCharts(); // 데이터 처리 완료 후 차트 업데이트
        });
}

function renderCards(cardData, flag) {
    // flag >> 0: 멤버의 카드, 1: 즐겨찾기 카드
    const myCardGrid = document.getElementById('myCardGrid');
    const likeCardGrid = document.getElementById('likeCardGrid');

    console.log('render card: ' + flag);

    cardData.forEach(card => {

        console.log(card.tag);
        const thumbUrl = getYoutubeThumbnail(card.url);
        let difficulty;
        let levelKor;
        let tagList = card.tag.split(',');
        switch (card.level) {
            case '1':
                difficulty = 'beginner';
                levelKor = '초급';
                break;
            case '2':
                difficulty = 'intermediate';
                levelKor = '중급';
                break;
            case '3':
                difficulty = 'advanced';
                levelKor = '고급';
                break;
            default:
                break;
        }

        let cardHtml = `
                    <div class="card" onclick="location.href='/mochilearn/page/studyCard?cardId=${card.id}'">
                        <div class="card-thumbnail">
                            <!-- 썸네일 공간 -->
                            <img class="thumbnail-image" src="${thumbUrl}" alt="썸네일 이미지" />
                            <div class="play-icon">▶</div>
                        </div>
                        <div class="video-info">
                            <h3 class="video-title">${card.title}</h3>
                            <div class="video-meta">
                                <span class="difficulty-tag ${difficulty}">${levelKor}</span>
                                <div class="video-stats">
                                    <span class="star">⭐</span> ${card.like}
                                </div>
                            </div>
                            <div class="tag-container">
                            `;

        tagList.forEach(tag => {
            cardHtml += `<span class="genre-tag-item">${tag}</span>`;
        });
        cardHtml += `</div>
                        </div>
                    </div>`;

        if(flag == 0) {
            myCardGrid.innerHTML += cardHtml;

        } else {
            likeCardGrid.innerHTML += cardHtml;

        }
    })
}

function getYoutubeThumbnail(youtubeUrl) {
    let videoId = '';
    if (!youtubeUrl) {
        return '/images/default-thumbnail.png';
    }

    // 쇼츠 URL (youtube.com/shorts/) 처리
    if (youtubeUrl.includes('youtube.com/shorts/')) {
        videoId = youtubeUrl.split('youtube.com/shorts/')[1].split(/[?&]/)[0];
    }
    // 짧은 URL (youtu.be/) 처리
    else if (youtubeUrl.includes('youtu.be/')) {
        videoId = youtubeUrl.split('youtu.be/')[1].split(/[?&]/)[0];
    }
    // 일반 URL (v=) 처리
    else if (youtubeUrl.includes('v=')) {
        videoId = youtubeUrl.split('v=')[1].split(/[?&]/)[0];
    }

    if (videoId) {
        return `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
    }

    return '/images/default-thumbnail.png';
}

function renderQuizLogs(data) {
    console.log('퀴즈 응시 로그 출력');
    const quizLogs = data;
    const container = document.querySelector('.quiz-logs-container');
    container.innerHTML = '';

    quizLogs.forEach(log => {

        const attemptItem = document.createElement('div');
        attemptItem.className = 'quiz-attempt-item';

        const quizSummary = document.createElement('div');
        quizSummary.className = 'quiz-summary';

        const formattedDate = new Date(log.attemptDate).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
        const levelName = getLevelName(log.level);

        quizSummary.innerHTML = `
                    <div>
                        <span>${formattedDate}</span>
                    </div>
                    <div>
                        <span>레벨: ${levelName}</span>
                    </div>
                    <div>
                        <span>점수: ${log.score} / 5</span>
                        <span class="quiz-details-toggle">▼</span>
                    </div>
                `;

        const quizDetailContent = document.createElement('div');
        quizDetailContent.className = 'quiz-detail-content';

        const quizzes = JSON.parse(log.quizData);
        const userAnswer = log.userAnswers;

        quizzes.forEach((quiz, index) => {
            let quizArea = document.createElement('div');
            quizArea.className = 'quiz-area';
            quizArea.innerHTML = `<div class="question-num">문제 ${index+1}</div>`;
            //console.log(quizArea);
            //console.log(quiz);
            restoreQuizLogs(quiz, userAnswer[index], quizArea);
            quizDetailContent.appendChild(quizArea);
        });

        //quizDetailContent.innerHTML = log.quizData;

        attemptItem.appendChild(quizSummary);
        attemptItem.appendChild(quizDetailContent);

        attemptItem.addEventListener('click', () => {
            quizDetailContent.classList.toggle('active');
            quizSummary.querySelector('.quiz-details-toggle').classList.toggle('open');
        })

        container.appendChild(attemptItem);
    });

}

function getLevelName(level) {
    switch(level) {
        case 1: return '초급';
        case 2: return '중급';
        case 3: return '고급';
        default: return '알 수 없음';
    }
}


function restoreQuizLogs(quiz, userAnswer, quizArea) {
    console.log(quiz);
    //console.log(quizArea);
    let answerArea = document.createElement('div');
    answerArea.className = 'answer-area';
    const answer = quiz.shuffleAnswer || quiz.blankAnswer || quiz.choiceAnswer;
    switch (quiz.quizType) {
        case 'SHUFFLE':
        case 'BLANK':
            renderDragDropQuiz(quiz.quizType, quiz, quizArea);
            break;
        case 'CHOICE':
            renderMultipleChoiceQuiz(quiz, quizArea);
            break;
    }
    restoreAnswer(answer, quiz.korean, userAnswer, answerArea);
    quizArea.appendChild(answerArea);
}

const renderDragDropQuiz = (type, quiz, quizArea) => {
    let blankCounter = 0;
    let answerHTML = '';
    let choices = [];
    let answerAreaContainerClass = 'blank-area';

    if (type === 'SHUFFLE') {
        answerHTML = quiz.shuffleAnswer.map(() =>
            `<div class="quizBlank" data-blank-index="${blankCounter++}"></div>`
        ).join('');
        choices = quiz.shuffledSentence;
        userAnswer = Array(quiz.shuffleAnswer.length).fill(null);
    } else { // BLANK
        answerAreaContainerClass = 'blank-sentence';
        answerHTML = quiz.blankSentence.map(part =>
            part === '______'
                ? `<div class="quizBlank" data-blank-index="${blankCounter++}"></div>`
                : `<span class="blank-word">${part}</span>`
        ).join('');
        choices = quiz.blankChoices;
        userAnswer = Array(quiz.blankAnswer.length).fill(null);
    }

    quizArea.innerHTML += `
            <div class="${answerAreaContainerClass}">${answerHTML}</div>
            <div class="quizBlankWordPool"></div>
        `;

    const wordPool = quizArea.querySelector('.quizBlankWordPool');
    choices.forEach((word, index) => {
        const item = document.createElement('div');
        item.className = 'wordPoolItem';
        item.textContent = word;
        item.dataset.originalIndex = index; // 원래 순서를 저장
        wordPool.appendChild(item);
    });
};

const renderMultipleChoiceQuiz = (quiz, quizArea) => {
    console.log(quizArea);

    const choiceSentences = document.createElement('div');
    choiceSentences.className = 'choice-sentences';
    choiceSentences.id = 'choiceSentences';

    console.log(choiceSentences);
    quiz.choiceSentences.forEach(sentence => {
        const btn = document.createElement('button');
        btn.className = 'choice-btn';
        btn.textContent = sentence;
        btn.onclick = () => selectMultipleChoice(btn);
        choiceSentences.appendChild(btn);
        quizArea.appendChild(choiceSentences);
    });
};

/*
function restoreAnswer(answer, korean, userAnswer, answerArea) {
    console.log(typeof answer);
    let finalAnswer;
    let finalUserAnswer;
    if(typeof answer != "string") {
        finalAnswer = answer.join(" \t ");
        const tempArr = userAnswer.answer.split(",");
        finalUserAnswer = (tempArr).join(" \t ");
        console.log(finalAnswer);
    }
    answerArea.innerHTML = `
        <p class="korean">${korean}</p>
        <p class="correct-answer">정답: ${finalAnswer}</p>
        <p class="user-answer">내 답안: ${finalUserAnswer}</p>
        `;

}*/
function restoreAnswer(answer, korean, userAnswer, answerArea) {
    console.log(typeof answer);
    let finalAnswer = "";
    let finalUserAnswer = "";

    if (Array.isArray(answer)) {
        // 정답 배열 → 각 요소를 <div>로 감쌈
        finalAnswer = answer.map(item => `<span class="answer-word-item">${item}</span>`).join("");

        // 유저 답안도 배열처럼 처리
        const tempArr = userAnswer.answer.split(",");
        finalUserAnswer = tempArr.map(item => `<span class="answer-word-item">${item.trim()}</span>`).join("");
    } else if (typeof answer === "string") {
        // 문자열일 경우 그대로 출력
        finalAnswer = answer;
        finalUserAnswer = userAnswer.answer;
    }

    answerArea.innerHTML = `
        <p class="korean">${korean}</p>
        <p class="correct-answer">정답: ${finalAnswer}</p>
        <p class="user-answer">내 답안: ${finalUserAnswer}</p>
    `;
}

