
// 마이크를 사용하여 사용자의 발음을 녹음하고, 평가 서버로 전송하는 기능을 담당합니다.

let mediaRecorder;
let audioChunks = [];
let micWrap;
let hint;
let isListening = false;
let audioContext = null;
let analyser = null;
let sourceNode = null;
let rafId = null;
let micBtn;
let lastPulseAt = 0;

let practiceComment;
let scoreContainer;
let accuracyScoreElem;
let fluencyScoreElem;
let feedbackMessageContainer;
let feedbackMessageElem;

// 점수 막대그래프 요소
let accuracyBar;
let fluencyBar;

const THRESHOLD = 0.02;      // 감지 임계값 (0 ~ 1). 필요시 조절하세요.
const MIN_PULSE_INTERVAL = 300; // 밀리초 단위: 연속 펄스 제한

/**
 * 발음 평가 관련 UI 컨트롤(버튼)의 이벤트 리스너 초기화
 */
function initializeSpeechPracticeListeners() {

    // UI 토글
    micBtn = document.getElementById('micBtn');
    micWrap = document.getElementById('micWrap');
    hint = document.getElementById('hint');

    practiceComment = document.getElementById('practiceComment');
    scoreContainer = document.getElementById('scoreContainer');
    accuracyScoreElem = document.getElementById('accuracyScore');
    fluencyScoreElem = document.getElementById('fluencyScore');
    feedbackMessageContainer = document.getElementById('feedbackMessageContainer');
    feedbackMessageElem = document.getElementById('feedbackMessage');

    accuracyBar = document.getElementById('accuracyBar');
    fluencyBar = document.getElementById('fluencyBar');

    console.log(micBtn);
    micBtn.addEventListener('click', async (e) => {

        const isLogin = await fetch('/mochilearn/api/user/session').then(res => res.json());

        if (!isLogin.loggedIn) {
            if (confirm('로그인 후 말하기 점수를 확인해보세요!')) {
                window.location.href = '/mochilearn/member/loginForm?redirect=' + encodeURIComponent(window.location.href);
            }
            return;
        }

        if (!isListening) {
            await startRecording();
        } else {
            stopRecording();
        }
    });
}

/**
 * 마이크 녹음 시작
 */
async function startRecording() {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });

        audioChunks = []; // 녹음 데이터 초기화
        mediaRecorder.ondataavailable = event => audioChunks.push(event.data);
        mediaRecorder.onstop = sendAudioToServer; // 녹음 중지 시 서버로 전송

        mediaRecorder.start();
        console.log("녹음 시작");

        audioContext = new window.AudioContext();
        analyser = audioContext.createAnalyser();
        analyser.fftSize = 2048;
        analyser.smoothingTimeConstant = 0.6;

        sourceNode = audioContext.createMediaStreamSource(stream);
        sourceNode.connect(analyser);

        isListening = true;
        micWrap.classList.add('listening');
        micBtn.setAttribute('aria-pressed','true');
        hint.textContent = '감지 중... 다시 버튼을 클릭하면 점수를 측정합니다.';

        monitorVolume();

    } catch (err) {
        console.error("마이크 접근에 실패했습니다:", err);
        alert("마이크 접근 권한이 필요합니다. 브라우저 설정을 확인해주세요.");
    }


}

/**
 * 녹음을 중지
 */
function stopRecording() {
    isListening = false;
    micWrap.classList.remove('listening');
    micBtn.setAttribute('aria-pressed','false');
    hint.textContent = '마이크가 중지되었습니다. 아이콘을 클릭해 다시 시작하세요.';

    if (rafId) { cancelAnimationFrame(rafId); rafId = null; }

    if (sourceNode) { try { sourceNode.disconnect(); } catch(e){} sourceNode = null; }
    if (analyser) { try { analyser.disconnect(); } catch(e){} analyser = null; }

    if (mediaRecorder) {
        mediaRecorder.stop();

        console.log("녹음 중지");
    }
}

/**
 * 녹음된 오디오 파일을 평가 서버로 전송
 */
async function sendAudioToServer() {
    if (audioChunks.length === 0) {
        hint.textContent = '녹음된 내용이 없습니다. 다시 시도해주세요.';
        return;
    }

    const audioBlob = new Blob(audioChunks, { type: 'audio/wav' });
    const referenceText = currentTranscript[currentTranscriptIndex]?.japanese;

    if (!referenceText) {
        alert("평가할 일본어 자막이 없습니다.");
        return;
    }

    const formData = new FormData();
    formData.append('audioFile', audioBlob, 'audio.wav');
    formData.append('referenceText', referenceText);

    try {
        const response = await fetch('http://localhost:5001/api/speech', {
            method: 'POST',
            body: formData,
        });

        if (response.ok) {
            const result = await response.json();
            displayResult(result);
        } else {
            console.error('Server error:', response.statusText);
            hint.textContent = "발음 평가 서버에서 오류가 발생했습니다.";
        }
    } catch (error) {
        console.error('Network error:', error);
        hint.textContent = "발음 평가 서버에 연결할 수 없습니다.";
    }

}

function displayResult(result) {
    // 숫자 점수 업데이트
    const accuracyScore = Math.round(result.accuracyScore);
    const fluencyScore = Math.round(result.fluencyScore);

    accuracyScoreElem.textContent = accuracyScore;
    fluencyScoreElem.textContent = fluencyScore;
    scoreContainer.classList.remove('hidden');

    // 막대 그래프와 숫자 색상 업데이트
    accuracyScoreElem.style.color = getScoreColor(accuracyScore);
    fluencyScoreElem.style.color = getScoreColor(fluencyScore);
    accuracyBar.style.width = `${accuracyScore}%`;
    fluencyBar.style.width = `${fluencyScore}%`;
    accuracyBar.style.backgroundColor = getScoreColor(accuracyScore);
    fluencyBar.style.backgroundColor = getScoreColor(fluencyScore);

    let feedbackHtml = '';
    let lowestAccuracyWord = null;
    let omittedWords = [];
    if (result.words) {
        result.words.forEach(word => {
            if (word.errorType === 'Omission') {
                omittedWords.push(word.word);
            }
            if (word.accuracyScore > 0 && (lowestAccuracyWord === null || word.accuracyScore < lowestAccuracyWord.accuracyScore)) {
                lowestAccuracyWord = word;
            }
        });
    }

    if (omittedWords.length > 0) {
        feedbackHtml += `<p><strong>🚨 빠뜨린 단어:</strong> ${omittedWords.join(', ')}</p>`;
    }

    if (lowestAccuracyWord && lowestAccuracyWord.accuracyScore < 80) {
        feedbackHtml += `<p><strong>💡 발음 유의:</strong> '${lowestAccuracyWord.word}'의 발음을 더 신경 써보세요!</p>`;
    } else if (accuracyScore > 80) {
        feedbackHtml += `<p><strong>🎉 아주 잘했어요!</strong> 훌륭한 발음입니다.</p>`;
    } else {
        feedbackHtml += `<p>다시 한번 도전해보세요! 화이팅!</p>`;
    }

    if (accuracyScore === 0 && fluencyScore === 0 && omittedWords.length === 0) {
        feedbackHtml = `<p>아직 녹음된 내용이 없거나 발음이 감지되지 않았습니다. 다시 한번 시도해주세요.</p>`;
    }

    feedbackMessageElem.innerHTML = feedbackHtml;
    feedbackMessageContainer.style.display = 'block';

    hint.textContent = '평가 결과입니다! 다시 말해보려면 마이크를 누르세요.';
}

// RMS 계산 (float time domain 데이터)
function calcRMS(float32Array) {
    let sum = 0;
    for (let i = 0; i < float32Array.length; i++){
        const v = float32Array[i];
        sum += v * v;
    }
    return Math.sqrt(sum / float32Array.length);
}

// 파동(펄스) 생성 함수
function createPulse() {
    const now = Date.now();
    if (now - lastPulseAt < MIN_PULSE_INTERVAL) return; // 너무 빠른 연속 생성 방지
    lastPulseAt = now;

    const pulse = document.createElement('div');
    pulse.className = 'pulse';
    micWrap.appendChild(pulse);

    // 애니메이션이 끝나면 제거
    pulse.addEventListener('animationend', () => {
        pulse.remove();
    }, { once: true });
}

// 감지 루프
function monitorVolume() {
    const bufferLen = analyser.fftSize;
    const data = new Float32Array(bufferLen);

    function loop() {
        analyser.getFloatTimeDomainData(data);
        const rms = calcRMS(data); // 0 ~ 1 범위(대략)

        // 임계값 넘어가면 펄스 생성
        if (rms > THRESHOLD) {
            createPulse();
        }

        rafId = requestAnimationFrame(loop);
    }
    rafId = requestAnimationFrame(loop);
}

function getScoreColor(score) {
    if (score >= 95) return 'var(--score-color-perfect)';
    if (score >= 75) return 'var(--score-color-excellent)';
    if (score >= 50) return 'var(--score-color-good)';
    if (score >= 25) return 'var(--score-color-fair)';
    if (score > 0) return 'var(--score-color-poor)';
    return 'var(--score-color-fail)';
}

// 페이지 벗어날 때 정리
window.addEventListener('beforeunload', () => {
    if (isListening) stopRecording();
});
