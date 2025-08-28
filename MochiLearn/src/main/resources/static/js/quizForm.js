let isFinished = false;
let isLoaded = false;

document.addEventListener('DOMContentLoaded', () => {
    // --- 전역 변수 및 상태 관리 ---
    let quizzes = [];
    let currentQuizIndex = -1;
    let userAnswer = [];
    let userResults = [];

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
            case 'SHUFFLE': renderScrambleQuiz(quiz); break;
            case 'BLANK': renderFillInBlankQuiz(quiz); break;
            case 'CHOICE': renderMultipleChoiceQuiz(quiz); break;
        }
    };

    const renderScrambleQuiz = (quiz) => {
        let blankCounter = 0;
        const answerHTML = quiz.shuffleAnswer.map(() => `<span class="blank-space" data-blank-index="${blankCounter++}"></span>`).join('');
        quizArea.innerHTML = `<div id="answer-area" class="answer-area">${answerHTML}</div><div id="word-bank" class="word-bank"></div>`;
        const wordBank = document.getElementById('word-bank');
        quiz.shuffledSentence.forEach((word, index) => {
            const btn = createWordButton(word, `bank-word-${index}`);
            wordBank.appendChild(btn);
        });
        userAnswer = Array(blankCounter).fill(null);
        setupInteractions();
    };

    const renderFillInBlankQuiz = (quiz) => {
        let blankCounter = 0;
        const sentenceHTML = quiz.blankSentence.map(part =>
            part === '______'
                ? `<span class="blank-space" data-blank-index="${blankCounter++}"></span>`
                : `<span class="blank-word">${part}</span>`
        ).join('');
        quizArea.innerHTML = `<div class="blank-sentence">${sentenceHTML}</div><div id="word-bank" class="blank-choices"></div>`;
        const choicesContainer = document.getElementById('word-bank');
        quiz.blankChoices.forEach((choice, index) => {
            const btn = createWordButton(choice, `bank-word-${index}`);
            choicesContainer.appendChild(btn);
        });
        userAnswer = Array(blankCounter).fill(null);
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
        btn.className = 'word-btn';
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
        const answerArea = document.getElementById('answer-area') || document.querySelector('.blank-sentence');

        // 클릭 이벤트 처리
        wordBank.addEventListener('click', (e) => {
            if (e.target.classList.contains('word-btn') && !e.target.classList.contains('used')) {
                const firstEmptyBlank = answerArea.querySelector('.blank-space:not(:has(*))');
                if (firstEmptyBlank) {
                    moveWordToBlank(e.target, firstEmptyBlank);
                }
            }
        });

        // 드래그 앤 드롭 이벤트 처리
        const draggables = wordBank.querySelectorAll('.word-btn');
        const dropzones = answerArea.querySelectorAll('.blank-space');

        draggables.forEach(draggable => {
            draggable.addEventListener('dragstart', (e) => {
                e.target.classList.add('dragging');
                e.dataTransfer.setData('text/plain', e.target.id);
            });
            draggable.addEventListener('dragend', (e) => {
                e.target.classList.remove('dragging');
            });
        });

        dropzones.forEach(dropzone => {
            dropzone.addEventListener('dragover', e => {
                e.preventDefault();
                if (!dropzone.hasChildNodes()) e.target.classList.add('drag-over');
            });
            dropzone.addEventListener('dragleave', e => e.target.classList.remove('drag-over'));
            dropzone.addEventListener('drop', e => {
                e.preventDefault();
                e.target.classList.remove('drag-over');
                const id = e.dataTransfer.getData('text/plain');
                const draggable = document.getElementById(id);
                if (dropzone.hasChildNodes() || !draggable || draggable.classList.contains('used')) return;
                moveWordToBlank(draggable, dropzone);
            });
        });
    };

    const moveWordToBlank = (wordButton, blankSpace) => {
        blankSpace.appendChild(wordButton);
        wordButton.classList.add('used'); // 선택지에서 숨기기 위해 'used' 클래스 추가
        const blankIndex = parseInt(blankSpace.dataset.blankIndex);
        userAnswer[blankIndex] = wordButton.textContent;

        wordButton.onclick = () => moveWordToBank(wordButton, blankSpace);
    };

    const moveWordToBank = (wordButton, blankSpace) => {
        const wordBank = document.getElementById('word-bank');
        wordBank.appendChild(wordButton);
        wordButton.classList.remove('used'); // 선택지에서 다시 보이도록 'used' 클래스 제거
        const blankIndex = parseInt(blankSpace.dataset.blankIndex);
        userAnswer[blankIndex] = null;
        wordButton.onclick = null;
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
    retryBtn.addEventListener('click', startQuiz);
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