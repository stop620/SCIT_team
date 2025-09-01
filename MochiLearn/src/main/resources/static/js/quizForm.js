let isFinished = false;
let isLoaded = false;

document.addEventListener('DOMContentLoaded', () => {
    // --- 전역 변수 및 상태 관리 ---
    let quizzes = [];
    let currentQuizIndex = -1;
    let userAnswer = [];
    let userResults = [];
    let originalWordBankHTML = ''; // 초기화 기능을 위한 원본 단어 목록 HTML 저장

    // --- DOM 요소 ---
    const quizView = document.getElementById('quiz-view');
    const resultView = document.getElementById('result-view');
    const progressBar = document.getElementById('progress-bar');
    const progressText = document.getElementById('progress-text');
    const koreanHint = document.getElementById('korean-hint');
    const quizArea = document.getElementById('quiz-area');
    const checkBtn = document.getElementById('check-btn');
    const nextBtn = document.getElementById('next-btn');
    const feedback = document.getElementById('feedback');
    const scoreText = document.getElementById('score-text');
    const incorrectList = document.getElementById('incorrect-list');
    const resultChartCanvas = document.getElementById('result-chart');
    const chartScore = document.getElementById('chart-score');
    const retryBtn = document.getElementById('retry-btn');
    const exitBtn = document.getElementById('exit-btn');

    const params = new URLSearchParams(window.location.search);
    const level = params.get("level");
    console.log('선택한 퀴즈 난이도: ', level);

    // --- 데이터 로딩 ---
    function loadQuizzes()  {
        if(isLoaded != true) {
            fetch(`/mochilearn/api/quiz?level=${level}`)
                .then(response => response.json())
                .then(data => {
                    quizzes = data;
                    console.log(quizzes);
                    isLoaded = true;
                    startQuiz();
                });
        } else {
            startQuiz();
        }
    };

    const startQuiz = () => {
        currentQuizIndex = -1;
        userResults = [];
        resultView.classList.add('hidden');
        quizView.classList.remove('hidden');
        nextQuestion();
    };

    // --- 퀴즈 렌더링 로직 ---
    const renderQuiz = () => {
        userAnswer = [];
        checkBtn.disabled = false;
        quizArea.innerHTML = '';
        const quiz = quizzes[currentQuizIndex];
        koreanHint.textContent = quiz.korean;

        switch (quiz.quizType) {
            case 'SHUFFLE': renderDraggableQuiz(quiz, 'SHUFFLE'); break;
            case 'BLANK': renderDraggableQuiz(quiz, 'BLANK'); break;
            case 'CHOICE': renderMultipleChoiceQuiz(quiz); break;
        }
    };

    const renderDraggableQuiz = (quiz, type) => {
        let blankCounter = 0;
        let answerHTML = '';
        let choices = [];

        if (type === 'SHUFFLE') {
            answerHTML = quiz.shuffleAnswer.map(() => `<div class="quizBlank" data-blank-index="${blankCounter++}"></div>`).join('');
            choices = quiz.shuffledSentence;
            userAnswer = Array(quiz.shuffleAnswer.length).fill(null);
        } else { // BLANK
            answerHTML = quiz.blankSentence.map(part =>
                part === '______'
                    ? `<div class="quizBlank" data-blank-index="${blankCounter++}"></div>`
                    : `<span class="blank-word">${part}</span>`
            ).join('');
            choices = quiz.blankChoices;
            userAnswer = Array(quiz.blankAnswer.length).fill(null);
        }

        quizArea.innerHTML = `
            <div id="answer-area" class="answer-area">${answerHTML}</div>
            <div id="word-bank" class="quizBlankWordPool"></div>
            <button id="resetDragDropBtn" class="reset-btn">초기화</button>
        `;

        const wordBank = document.getElementById('word-bank');
        choices.forEach((word, index) => {
            const btn = createWordButton(word, `bank-word-${index}`);
            wordBank.appendChild(btn);
        });

        originalWordBankHTML = wordBank.innerHTML; // 초기화용 HTML 저장
        setupInteractions();
    };

    const renderMultipleChoiceQuiz = (quiz) => {
        quizArea.innerHTML = '<div id="choice-sentences" class="choice-sentences"></div>';
        const choicesContainer = document.getElementById('choice-sentences');
        quiz.choiceSentences.forEach(sentence => {
            const btn = document.createElement('button');
            btn.className = 'choice-btn';
            btn.textContent = sentence;
            btn.onclick = () => selectMultipleChoice(btn);
            choicesContainer.appendChild(btn);
        });
    };

    // --- 상호작용 로직 ---
    const createWordButton = (word, id) => {
        const btn = document.createElement('button');

        btn.className = 'wordPoolItem';
        btn.textContent = word;
        btn.id = id;
        btn.draggable = true;
        return btn;
    };

    const selectMultipleChoice = (button) => {
        const selected = document.querySelector('#choice-sentences .selected');
        if (selected) selected.classList.remove('selected');
        button.classList.add('selected');
        userAnswer = [button.textContent];
    };

    // --- 드래그 앤 드롭 및 클릭 통합 로직 ---
    const setupInteractions = () => {
        const wordBank = document.getElementById('word-bank');
        const answerArea = document.getElementById('answer-area');
        const resetBtn = document.getElementById('resetDragDropBtn');

        // 클릭 이벤트 처리 (단어 선택)
        wordBank.addEventListener('click', (e) => {
            if (e.target.classList.contains('wordPoolItem')) {
                const firstEmptyBlank = answerArea.querySelector('.quizBlank:not(:has(*))');
                if (firstEmptyBlank) {
                    moveWordToBlank(e.target, firstEmptyBlank);
                }
            }
        });

        // 클릭 이벤트 처리 (단어 초기화)
        answerArea.addEventListener('click', (e) => {
            if (e.target.classList.contains('wordPoolItem')) {
                moveWordToBank(e.target);
            }
        });

        // 드래그 앤 드롭 이벤트 처리
        const draggables = document.querySelectorAll('.wordPoolItem');
        const droppables = document.querySelectorAll('.quizBlank');

        draggables.forEach(item => {
            item.addEventListener('dragstart', dragStart);
            item.addEventListener('dragend', dragEnd);
        });

        droppables.forEach(item => {
            item.addEventListener('dragover', dragOver);
            item.addEventListener('dragleave', dragLeave);
            item.addEventListener('drop', dragDrop);
        });

        // 초기화 버튼 이벤트
        resetBtn.addEventListener('click', resetDraggableQuiz);
    };

    function dragStart(ev) {
        ev.dataTransfer.effectAllowed = "move";
        ev.dataTransfer.setData("text/plain", ev.target.id);
        ev.target.classList.add('dragging');
    }

    function dragEnd(ev) {
        ev.target.classList.remove('dragging');
    }

    function dragOver(ev) {
        ev.preventDefault();
        const dropzone = ev.currentTarget;
        if (!dropzone.hasChildNodes()) {
            dropzone.classList.add('drag-over');
        }
    }

    function dragLeave(ev) {
        ev.currentTarget.classList.remove('drag-over');
    }

    function dragDrop(ev) {
        ev.preventDefault();
        const dropzone = ev.currentTarget;
        const draggingItemId = ev.dataTransfer.getData("text/plain");
        const draggingItem = document.getElementById(draggingItemId);

        dropzone.classList.remove('drag-over');

        // 빈칸에만 드롭 가능하도록 수정
        if (draggingItem && !dropzone.hasChildNodes()) {
            moveWordToBlank(draggingItem, dropzone);
        }
    }

    const moveWordToBlank = (wordButton, blankSpace) => {
        blankSpace.appendChild(wordButton);
        const blankIndex = parseInt(blankSpace.dataset.blankIndex);
        userAnswer[blankIndex] = wordButton.textContent;
    };

    const moveWordToBank = (wordButton) => {
        const wordBank = document.getElementById('word-bank');

        // 정답 배열에서 해당 단어 제거
        const wordToRemove = wordButton.textContent;
        const indexInAnswer = userAnswer.findIndex(ans => ans === wordToRemove);
        if(indexInAnswer > -1) userAnswer[indexInAnswer] = null;

        wordBank.appendChild(wordButton);
    };

    const resetDraggableQuiz = () => {
        const wordBank = document.getElementById('word-bank');
        const answerArea = document.getElementById('answer-area');

        // 모든 단어를 word-bank로 되돌림
        answerArea.querySelectorAll('.wordPoolItem').forEach(item => {
            wordBank.appendChild(item);
        });

        // 원래 순서대로 복원
        wordBank.innerHTML = originalWordBankHTML;

        // 정답 배열 초기화
        userAnswer.fill(null);

        // 이벤트 리스너 다시 연결
        setupInteractions();
    };


    // --- 정답 확인 및 다음 문제 ---
    const checkAnswer = () => {
        const quiz = quizzes[currentQuizIndex];
        let isCorrect = false;

        switch (quiz.quizType) {
            case 'SHUFFLE':
                isCorrect = JSON.stringify(userAnswer) === JSON.stringify(quiz.shuffleAnswer);
                break;
            case 'BLANK':
                isCorrect = JSON.stringify(userAnswer) === JSON.stringify(quiz.blankAnswer);
                break;
            case 'CHOICE':
                isCorrect = userAnswer[0] === quiz.choiceAnswer;
                break;
        }

        userResults.push({ quiz, isCorrect, userAnswer: [...userAnswer] });

        feedback.textContent = isCorrect ? '정답입니다!' : '틀렸습니다!';
        feedback.className = `feedback ${isCorrect ? 'correct' : 'incorrect'}`;
        quizView.className = `quiz-container ${isCorrect ? 'correct-flash' : 'incorrect-flash'}`;
        setTimeout(() => quizView.classList.remove('correct-flash', 'incorrect-flash'), 500);

        checkBtn.style.display = 'none';
        nextBtn.style.display = 'block';
        checkBtn.disabled = true;
    };

    const nextQuestion = () => {
        if (currentQuizIndex >= quizzes.length - 1) {
            progressBar.style.width = '100%';
            progressText.textContent = `완료!`;
            setTimeout(showResults, 500);
            return;
        }
        currentQuizIndex++;
        progressText.textContent = `${currentQuizIndex + 1} / ${quizzes.length}`;
        progressBar.style.width = `${((currentQuizIndex) / quizzes.length) * 100}%`;
        feedback.textContent = '';
        checkBtn.style.display = 'block';
        nextBtn.style.display = 'none';
        renderQuiz();
    };

    // --- 결과창 로직 ---
    const showResults = () => {
        quizView.classList.add('hidden');
        resultView.classList.remove('hidden');

        const correctCount = userResults.filter(r => r.isCorrect).length;
        const totalCount = quizzes.length;
        scoreText.textContent = `총 ${totalCount}문제 중 ${correctCount}문제를 맞혔어요!`;
        chartScore.textContent = `${correctCount}/${totalCount}`;

        const incorrectQuizzes = userResults.filter(r => !r.isCorrect);
        if (incorrectQuizzes.length > 0) {
            incorrectList.innerHTML = incorrectQuizzes.map(item => {
                const quiz = item.quiz;
                let correctAnswerText = '';
                let userAnswerText = item.userAnswer.filter(Boolean).join('');

                if (quiz.quizType === 'SHUFFLE') correctAnswerText = quiz.shuffleAnswer.join('');
                if (quiz.quizType === 'BLANK') correctAnswerText = quiz.blankAnswer.join(', ');
                if (quiz.quizType === 'CHOICE') correctAnswerText = quiz.choiceAnswer;

                return `<div class="incorrect-item">
                                <p class="korean">${quiz.korean}</p>
                                <p class="user-answer">내 답안: ${userAnswerText || '(미입력)'}</p>
                                <p class="correct-answer">정답: ${correctAnswerText}</p>
                            </div>`;
            }).join('');
        } else {
            incorrectList.innerHTML = '<p style="text-align:center;">틀린 문제가 없어요! 완벽해요! 🎉</p>';
        }

        new Chart(resultChartCanvas, {
            type: 'doughnut',
            data: {
                labels: ['정답', '오답'],
                datasets: [{
                    data: [correctCount, totalCount - correctCount],
                    backgroundColor: ['#4c6ef5', '#fa5252'],
                    borderColor: '#ffffff',
                    borderWidth: 4,
                }]
            },
            options: {
                responsive: true,
                cutout: '70%',
                plugins: { legend: { display: false }, tooltip: { enabled: false } }
            }
        });
        if(isFinished != true) {
            sendQuizResult();

        }
    };

    // --- 이벤트 리스너 ---
    checkBtn.addEventListener('click', checkAnswer);
    nextBtn.addEventListener('click', nextQuestion);
    retryBtn.addEventListener('click', () => {
        sessionStorage.removeItem('quizFinished');
        startQuiz();
    });
    exitBtn.addEventListener('click', () => {
        alert('나가기 버튼 클릭됨');
    });

    function sendQuizResult() {
        const resultData = userResults.map(result => {
            const quiz = result.quiz;
            const quizId = quiz.quizId;
            const isCorrect = result.isCorrect;
            const userAnswer = result.userAnswer;

            let correctAnswer = [];
            switch (quiz.quizType) {
                case 'SHUFFLE':
                    correctAnswer = quiz.shuffleAnswer || [];
                    break;
                case 'BLANK':
                    correctAnswer = quiz.blankAnswer || [];
                    break;
                case 'CHOICE':
                    correctAnswer = quiz.choiceAnswer ? [quiz.choiceAnswer] : [];
                    break;
            }

            return {
                quizId,
                isCorrect,
                quiz,
                userAnswer,
                level,
            };
        });

        console.log(resultData);

        fetch(`/mochilearn/api/quiz/saveResult`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(resultData)
        })
            .then(response => response.ok ? response.json() : Promise.reject('서버 응답 오류: ' + response.statusText))
            .then(data => {
                console.log('퀴즈 결과가 저장되었습니다.');
                isFinished = true;
            })
            .catch(error => {
                alert('결과 저장 중 오류가 발생했습니다: ' + (error.message || error));
                console.error('Result Save Error:', error);
            });
    }


    // --- 초기화 ---
    loadQuizzes();
});