
// 마이크를 사용하여 사용자의 발음을 녹음하고, 평가 서버로 전송하는 기능을 담당합니다.

let mediaRecorder;
let audioChunks = [];

/**
 * 발음 평가 관련 UI 컨트롤(버튼)의 이벤트 리스너 초기화
 */
function initializeSpeechPracticeListeners() {
    $('#startButton').click(startRecording);
    $('#stopButton').click(stopRecording);
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
        $('#startButton').prop('disabled', true);
        $('#stopButton').prop('disabled', false);
        console.log("녹음 시작");

    } catch (err) {
        console.error("마이크 접근에 실패했습니다:", err);
        alert("마이크 접근 권한이 필요합니다. 브라우저 설정을 확인해주세요.");
    }
}

/**
 * 녹음을 중지
 */
function stopRecording() {
    if (mediaRecorder) {
        mediaRecorder.stop();
        $('#startButton').prop('disabled', false);
        $('#stopButton').prop('disabled', true);
        console.log("녹음 중지");
    }
}

/**
 * 녹음된 오디오 파일을 평가 서버로 전송
 */
async function sendAudioToServer() {
    if (audioChunks.length === 0) return;

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
            console.log('Pronunciation Assessment Result:', result);
            alert(`평가 결과:\n정확도: ${result.accuracyScore}\n유창성: ${result.fluencyScore}`);
        } else {
            console.error('Server error:', response.statusText);
            alert("발음 평가 서버에서 오류가 발생했습니다.");
        }
    } catch (error) {
        console.error('Network error:', error);
        alert("발음 평가 서버에 연결할 수 없습니다.");
    }
}
