document.addEventListener('DOMContentLoaded', () => {
    // --- 전역 변수 및 상태 관리 ---
    let quizzes = [];
    let currentQuizIndex = -1;
    let userAnswer = [];
    let userResults = [];
    let isRetryMode = false;

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

    // --- 데이터 로딩 (sessionStorage 적용) ---
    function loadQuizzes() {
        const storedQuizzes = sessionStorage.getItem('quizzes');
        if (storedQuizzes && isRetryMode) {
            quizzes = JSON.parse(storedQuizzes);
            startQuiz();
        } else {
            fetch(`/mochilearn/api/quiz?level=${level}`)
                .then(response => response.json())
                .then(data => {
                    quizzes = data;
                    console.log(quizzes);
                    isRetryMode = false; // 다시풀기 모드 해제
                    sessionStorage.setItem('quizzes', JSON.stringify(data));

                    startQuiz();
                });
        }
    }

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
            case 'SHUFFLE':
            case 'BLANK':
                renderDragDropQuiz(quiz.quizType, quiz);
                break;
            case 'CHOICE':
                renderMultipleChoiceQuiz(quiz);
                break;
        }
    };

    const renderDragDropQuiz = (type, quiz) => {
        let blankCounter = 0;
        let answerHTML = '';
        let choices = [];
        let answerAreaContainerClass = 'answer-area';

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

        quizArea.innerHTML = `
            <div class="${answerAreaContainerClass}">${answerHTML}</div>
            <div class="quizBlankWordPool"></div>
            <button id="resetDragDropBtn" class="reset-btn">초기화</button>
        `;

        const wordPool = quizArea.querySelector('.quizBlankWordPool');
        choices.forEach((word, index) => {
            const item = document.createElement('div');
            item.className = 'wordPoolItem';
            item.textContent = word;
            item.draggable = true;
            item.dataset.originalIndex = index; // 원래 순서를 저장
            wordPool.appendChild(item);
        });

        setupDragDropInteractions();
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

    // --- 상호작용 로직 (이벤트 위임 방식) ---
    const setupDragDropInteractions = () => {
        const wordPool = quizArea.querySelector('.quizBlankWordPool');
        const answerArea = quizArea.querySelector('.answer-area, .blank-sentence');
        const draggables = quizArea.querySelectorAll('.wordPoolItem');
        const dropzones = quizArea.querySelectorAll('.quizBlank');
        const resetBtn = quizArea.querySelector('#resetDragDropBtn');

        // 클릭 이벤트 (이벤트 위임)
        quizArea.addEventListener('click', (e) => {
            if (e.target.classList.contains('wordPoolItem')) {
                const wordItem = e.target;
                if (wordItem.parentElement === wordPool) {
                    // 선택지에 있을 때 -> 빈칸으로 이동
                    const firstEmptyBlank = answerArea.querySelector('.quizBlank:not(:has(*))');
                    if (firstEmptyBlank) {
                        placeWord(wordItem, firstEmptyBlank);
                    }
                } else {
                    // 빈칸에 있을 때 -> 선택지로 이동
                    returnWordToPool(wordItem);
                }
            }
        });

        // 드래그 앤 드롭 이벤트
        draggables.forEach(item => {
            item.addEventListener('dragstart', dragStart);
            item.addEventListener('dragend', dragEnd);
        });

        dropzones.forEach(item => {
            item.addEventListener('dragover', dragOver);
            item.addEventListener('dragleave', dragLeave);
            item.addEventListener('drop', drop);
        });

        // 초기화 버튼 이벤트
        if (resetBtn) {
            resetBtn.addEventListener('click', () => {
                // 모든 단어를 순서대로 wordPool로 되돌림
                const allWords = Array.from(quizArea.querySelectorAll('.wordPoolItem'));
                allWords.sort((a, b) => a.dataset.originalIndex - b.dataset.originalIndex);
                allWords.forEach(word => wordPool.appendChild(word));

                // 정답 배열 초기화
                userAnswer.fill(null);
            });
        }
    };

    function dragStart(ev) {
        ev.target.classList.add('dragging');
        ev.dataTransfer.setData("text/plain", ev.target.textContent); // ID 대신 텍스트를 사용
    }

    function dragEnd(ev) {
        ev.target.classList.remove('dragging');
    }

    function dragOver(ev) {
        ev.preventDefault();
        if (ev.currentTarget.children.length === 0) {
            ev.currentTarget.classList.add('drag-over');
        }
    }

    function dragLeave(ev) {
        ev.currentTarget.classList.remove('drag-over');
    }

    function drop(ev) {
        ev.preventDefault();
        ev.currentTarget.classList.remove('drag-over');
        const wordText = ev.dataTransfer.getData("text/plain");
        const draggingItem = Array.from(quizArea.querySelectorAll('.wordPoolItem')).find(d => d.textContent === wordText);

        if (draggingItem && ev.currentTarget.children.length === 0) {
            placeWord(draggingItem, ev.currentTarget);
        }
    }

    function placeWord(wordItem, blank) {
        const blankIndex = parseInt(blank.dataset.blankIndex);
        userAnswer[blankIndex] = wordItem.textContent;
        blank.appendChild(wordItem);
    }

    function returnWordToPool(wordItem) {
        const wordPool = quizArea.querySelector('.quizBlankWordPool');
        const blank = wordItem.parentElement;
        if (blank && blank.classList.contains('quizBlank')) {
            const blankIndex = parseInt(blank.dataset.blankIndex);
            userAnswer[blankIndex] = null;
        }

        // 원래 순서에 맞게 되돌리기
        const originalIndex = parseInt(wordItem.dataset.originalIndex);
        const itemsInPool = Array.from(wordPool.children);
        let inserted = false;
        for (let i = 0; i < itemsInPool.length; i++) {
            if (parseInt(itemsInPool[i].dataset.originalIndex) > originalIndex) {
                wordPool.insertBefore(wordItem, itemsInPool[i]);
                inserted = true;
                break;
            }
        }
        if (!inserted) {
            wordPool.appendChild(wordItem);
        }
    }

    const selectMultipleChoice = (button) => {
        const selected = document.querySelector('#choice-sentences .selected');
        if (selected) selected.classList.remove('selected');
        button.classList.add('selected');
        userAnswer = [button.textContent];
    };

    // --- 정답 확인 및 다음 문제 ---
    const checkAnswer = () => {
        const isAnswered = userAnswer.filter(val => val !== null).length > 0;
        if (!isAnswered) {
            feedback.textContent = '정답을 선택해주세요!';
            feedback.className = 'feedback incorrect';
            quizArea.animate([{ transform: 'translateX(-5px)' }, { transform: 'translateX(5px)' }], { duration: 150, iterations: 2, direction: 'alternate' });
            return;
        }

        const quiz = quizzes[currentQuizIndex];
        let isCorrect = false;
        let correctAnswer;

        switch (quiz.quizType) {
            case 'SHUFFLE':
                correctAnswer = quiz.shuffleAnswer;
                isCorrect = JSON.stringify(userAnswer) === JSON.stringify(correctAnswer);
                break;
            case 'BLANK':
                correctAnswer = quiz.blankAnswer;
                isCorrect = JSON.stringify(userAnswer) === JSON.stringify(correctAnswer);
                break;
            case 'CHOICE':
                correctAnswer = quiz.choiceAnswer;
                isCorrect = userAnswer[0] === correctAnswer;
                break;
        }

        userResults.push({ quiz, isCorrect, userAnswer: [...userAnswer], correctAnswer });

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
            if (!isRetryMode) {
                // 최초 완료 시에만 결과 저장
                sendQuizResult();
            }
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
                let userAnswerText = Array.isArray(item.userAnswer) ? item.userAnswer.filter(Boolean).join(' ') : item.userAnswer;
                let correctAnswerText = Array.isArray(item.correctAnswer) ? item.correctAnswer.join('') : item.correctAnswer;

                return `<div class="incorrect-item">
                            <p class="korean">${item.quiz.korean}</p>
                            <p class="user-answer">내 답안: ${userAnswerText || '(미입력)'}</p>
                            <p class="correct-answer">정답: ${correctAnswerText}</p>
                        </div>`;
            }).join('');
        } else {
            incorrectList.innerHTML = '<p style="text-align:center;">틀린 문제가 없어요! 완벽해요! 🎉</p>';
        }

        if(window.resultChart instanceof Chart) window.resultChart.destroy();
        window.resultChart = new Chart(resultChartCanvas, {
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
            options: { cutout: '70%', plugins: { legend: { display: false }, tooltip: { enabled: false } } }
        });
    };

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

    // --- 이벤트 리스너 ---
    checkBtn.addEventListener('click', checkAnswer);
    nextBtn.addEventListener('click', nextQuestion);
    retryBtn.addEventListener('click', () => { isRetryMode = true; startQuiz(); });
    exitBtn.addEventListener('click', () => { window.location.href = '/mochilearn/page/quiz'; }); // 예시: 학습 페이지로 이동

    // --- 초기화 ---
    loadQuizzes();
});
