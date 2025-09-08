// 전역 변수
let player; // 유튜브 플레이어
let timelineTimeout;
const timelines = [];
let currentTranscript = [];
let currentTranscriptIndex = 0;
let subtitleInterval;
let liked = false;
let likeCount = 0;
let currentSectionStart = 0; // 현재 섹션 시작 시간 저장 변수 추가

// 임시 말하기 기능 전역 변수
const startButton = document.getElementById('startButton');
const stopButton = document.getElementById('stopButton');
let mediaRecorder;
let audioChunks = [];

// 언어 바꾸기 전역 변수
const langChange = document.getElementById('langChange');


function renderCard(cardData) {
    document.getElementById('cardTitle').innerText = cardData.title || "";
    document.getElementById('cardLike').innerText = cardData.like || "";

    const tagsContainer = document.querySelector('.tags.card');
    tagsContainer.innerHTML = ''; // 기존 태그 초기화

    // level 값 숫자 -> 한글 매핑
    const levelMap = {
        1: "초급",
        2: "중급",
        3: "고급"
    };

    if (cardData.level) {
        const levelText = levelMap[cardData.level] || "기본";
        const levelDiv = document.createElement('div');
        levelDiv.className = 'tag-item';
        levelDiv.textContent = levelText;
        tagsContainer.appendChild(levelDiv);
    }

    if (cardData.tag) {
        const tagList = cardData.tag.split(',').map(tag => tag.trim());
        tagList.forEach(tag => {
            if (tag) {
                const tagDiv = document.createElement('div');
                tagDiv.className = 'tag-item';
                tagDiv.textContent = tag;
                tagsContainer.appendChild(tagDiv);
            }
        });
    }
}

$(document).ready(function() {
    const urlParams = new URLSearchParams(window.location.search);
    const cardId = urlParams.get('cardId');

    console.log("🏷️ URL에서 추출한 cardId:", cardId);

    function updateLikeButton() {
        $('#cardLike').text(card.like);
        if (liked) {
            $('#like-button').css('color', 'red');
        } else {
            $('#like-button').css('color', 'black');
        }
    }

    if (cardId) {
        $.get(`/mochilearn/api/study/card?cardId=${cardId}`)
            .done(function(cardData) {
                card = cardData;
                card.videoId = extractVideoId(card.url);
                renderCard(card);
                renderSectionButtons(card.sections);

                liked = card.liked;
                updateLikeButton();

                if (typeof YT !== 'undefined' && YT && YT.Player) {
                    createPlayer(card.videoId);
                } else {
                    console.warn("⚠️ YouTube IFrame API가 아직 로드되지 않음");
                }
            })
            .fail(function(jqXHR, textStatus, errorThrown) {
                console.error("❌ API 호출 실패:", textStatus, errorThrown);
            });

        let isLoggedIn = false;

        // 페이지 로드시 로그인 상태 확인 API 호출
        $.get('/mochilearn/api/user/session')
            .done(function(userData) {
                isLoggedIn = !!(userData && userData.loggedIn);
            })
            .fail(function() {
                isLoggedIn = false;
            });

        // 좋아요 클릭 이벤트
        $('#like-button').click(function() {
            if (!isLoggedIn) {
                alert('좋아요를 누르려면 로그인해야 합니다.');
                return;
            }

            const newLiked = !liked;

            $.post('/mochilearn/api/study/likes/toggle', { cardId: cardId, liked: newLiked })
                .done(function(response) {
                    if ((response.status === 'liked' && newLiked) || (response.status === 'unliked' && !newLiked)) {
                        liked = newLiked;
                        card.like += liked ? 1 : -1;
                        $('#cardLike').text(card.like);
                        updateLikeButton();
                    } else {
                        alert('좋아요 상태 갱신 실패');
                    }
                })
                .fail(function() {
                    alert('좋아요 처리 중 오류');
                });
        });

        // 삭제 버튼 이벤트
        $('#delete-button').click(function() {
            if (!confirm("카드를 삭제하시겠습니까?")) return;
            $.ajax({
                url: `/mochilearn/api/study/card/${cardId}`,
                type: 'DELETE',
                success: function() {
                    alert("카드가 삭제되었습니다.");
                    window.location.href = "/mochilearn/page/study";
                },
                error: function() {
                    alert("카드 삭제에 실패했습니다.");
                }
            });
        });

        // 자막 버튼 이벤트
        $('#prev-btn').click(() => changeTranscriptIndex(-1));
        $('#next-btn').click(() => changeTranscriptIndex(1));
    } else {
        console.warn("⚠️ URL에 cardId 파라미터가 존재하지 않음");
    }

    //언어 변환 
    langChange.onclick = function(){ langChangeFunction(); };

});

// 시간 변환 헬퍼 함수
const parseTime = (timeString) => {
    const parts = timeString.split(':').map(Number);
    return parts.length === 2 ? parts[0] * 60 + parts[1] : 0;
};

const parseAITime = (timeValue) => {
    if (typeof timeValue === 'number') return timeValue;
    if (typeof timeValue === 'string') {
        const parts = timeValue.split(':');
        if (parts.length === 2) return (parseFloat(parts[0]) * 60) + parseFloat(parts[1]);
        return parseFloat(timeValue);
    }
    return 0;
};

const extractVideoId = (url) => {
    const regex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/|youtube\.com\/shorts\/)([^"&?\/\s]{11})/i;
    const match = url.match(regex);
    return match ? match[1] : null;
};

// YouTube API 준비 콜백
function onYouTubeIframeAPIReady() {
    if (card && card.videoId) {
        createPlayer(card.videoId);
    }
}

const createPlayer = (videoId) => {
    if (player) player.destroy();
    player = new YT.Player('player', {
        height: '100%',
        width: '100%',
        videoId: videoId,
        playerVars: { 'playsinline': 1, 'controls': 1, 'rel': 0 },
        events: { 'onStateChange': onPlayerStateChange }
    });
};

const onPlayerStateChange = (event) => {
    if (event.data === YT.PlayerState.PAUSED || event.data === YT.PlayerState.ENDED) {
        clearInterval(subtitleInterval);
        clearInterval(timelineTimeout);
    }
};

// UI 렌더링 함수
const updateSingleTranscriptLine = () => {
    const transcriptContainer = document.getElementById('transcript-container');
    const japaneseLineElement = document.getElementById('japanese-line');
    transcriptContainer.classList.toggle('hidden', currentTranscript.length === 0);
    japaneseLineElement.innerHTML = '';

    if (currentTranscript.length > 0) {
        const item = currentTranscript[currentTranscriptIndex];

        if (item.japaneseTokens && Array.isArray(item.japaneseTokens)) {
            item.japaneseTokens.forEach((token, index) => {
                const span = document.createElement('span');
                span.textContent = token.surface;
                span.className = 'japanese-token';
                span.dataset.index = index;
                span.tokenData = token;
                japaneseLineElement.appendChild(span);
            });
        } else {
            japaneseLineElement.textContent = item.japanese;
        }

        document.getElementById('korean-line').textContent = item.korean;
        document.getElementById('transcript-index').textContent = `${currentTranscriptIndex + 1} / ${currentTranscript.length}`;
    }
};

// 자막 인덱스 관련 (prev/next)
const changeTranscriptIndex = (direction) => {
    if (currentTranscript.length === 0) return;

    if (direction === -1) { // prev
        if (currentTranscriptIndex === 0) return;
        else currentTranscriptIndex -= 1;
    } else if (direction === 1) { // next
        if (currentTranscriptIndex === currentTranscript.length - 1) return;
        else currentTranscriptIndex += 1;
    } else {
        return;
    }

    updateSingleTranscriptLine();

    // 영상 재생 위치: 섹션 시작시간 + 자막 상대시간
    if (player && typeof player.seekTo === 'function') {
        const relativeTime = parseAITime(currentTranscript[currentTranscriptIndex].time);
        const absoluteTime = currentSectionStart + relativeTime;
        player.seekTo(absoluteTime, true);
    }
};

const renderTimelines = () => {
    const timelineContainer = document.getElementById('timeline-buttons-container');
    timelineContainer.innerHTML = '';

    timelines.forEach((timeline, index) => {
        const button = document.createElement('button');
        button.onclick = () => startTimelinePlayback(timeline);
        timelineContainer.appendChild(button);
    });
};

function renderSectionButtons(sections) {
    const container = document.getElementById('timeline-buttons-container');
    container.innerHTML = '';

    sections.forEach(section => {
        const button = document.createElement('button');
        button.className = 'timeline-button';
        button.textContent = section.section_num || section.sectionNum || "구간";
        button.setAttribute('data-section-id', section.id);

        button.addEventListener('click', () => {
            console.log(`🎯 섹션 버튼 클릭 - ID: ${section.id}, 번호: ${section.section_num || section.sectionNum}`);

            // 현재 섹션 시작 시간 저장
            currentSectionStart = section.start_seconds || 0;

            currentTranscript = section.sentences || [];
            currentTranscriptIndex = 0;
            updateSingleTranscriptLine();

            startTimelinePlayback({
                start: currentSectionStart,
                end: section.end_seconds,
                transcript: currentTranscript,
            });
        });

        container.appendChild(button);
    });
}

const startTimelinePlayback = (timeline) => {
    console.log("▶ startTimelinePlayback 실행, timeline 데이터:", timeline);
    if (player) {
        player.seekTo(timeline.start, true);
        player.playVideo();

        clearInterval(timelineTimeout);
        clearInterval(subtitleInterval);

        timelineTimeout = setInterval(() => {
            if (player && typeof player.getCurrentTime === 'function') {
                if (player.getCurrentTime() >= timeline.end) {
                    player.pauseVideo();
                    clearInterval(timelineTimeout);
                }
            }
        }, 100);

        currentTranscript = timeline.transcript;
        currentTranscriptIndex = 0;
        updateSingleTranscriptLine();

        subtitleInterval = setInterval(() => {
            if (!player || typeof player.getCurrentTime !== 'function') {
                clearInterval(subtitleInterval);
                return;
            }
            const currentTime = player.getCurrentTime();
            if (currentTime < timeline.start || currentTime > timeline.end) {
                clearInterval(subtitleInterval);
                return;
            }

            const relativeTime = currentTime - timeline.start;

            let newIndex = 0;
            for (let i = 0; i < currentTranscript.length; i++) {
                const itemTime = parseAITime(currentTranscript[i].time);
                const nextItemTime = (i + 1 < currentTranscript.length) ? parseAITime(currentTranscript[i + 1].time) : Infinity;

                if (relativeTime >= itemTime && relativeTime < nextItemTime) {
                    newIndex = i;
                    break;
                }
            }

            if (newIndex !== currentTranscriptIndex) {
                currentTranscriptIndex = newIndex;
                updateSingleTranscriptLine();
            }
        }, 100);
    }
};


// 말하기 기능 이벤트 리스너
startButton.addEventListener('click', async () => {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });

        audioChunks = []; // 녹음 시작 시 배열 초기화

        mediaRecorder.ondataavailable = event => {
            audioChunks.push(event.data);
        };

        mediaRecorder.onstop = async () => {
            const audioBlob = new Blob(audioChunks, { type: 'audio/wav' });
            console.log(audioBlob.size);
            // 서버로 음성 파일 전송
            const formData = new FormData();
            formData.append('audioFile', audioBlob, 'audio.wav'); // 파일 이름과 타입 지정
            formData.append('referenceText', '最近ついてないわって顔をしているそこのあなた。'); // 평가할 문장

            try {
                const response = await fetch('http://localhost:5001/api/speech', {
                    method: 'POST',
                    body: formData,
                });

                if (response.ok) {
                    const result = await response.json();
                    console.log('Pronunciation Assessment Result:', result);
                    alert(`정확도: ${result.accuracyScore}, 유창성: ${result.fluencyScore}`);
                } else {
                    console.error('Server error:', response.statusText);
                }
            } catch (error) {
                console.error('Network error:', error);
            }
        };

        mediaRecorder.start();
        startButton.disabled = true;
        stopButton.disabled = false;
        console.log("녹음 시작");

    } catch (err) {
        console.error("마이크 접근에 실패했습니다:", err);
    }
});

stopButton.addEventListener('click', () => {
    mediaRecorder.stop();
    startButton.disabled = false;
    stopButton.disabled = true;
    console.log("녹음 중지");
});

// 언어 변환 함수
function langChangeFunction(){
    let langChange = document.getElementById('langChange');
    let jp = document.querySelector('.jp')
    let kr = document.querySelector('.kr');
    let now;  // 0: 둘 다, 1: 일본어만, 2: 한국어만

    if(!kr.classList.contains('hidden') && !jp.classList.contains('hidden')){
        now = 0;
    }else if(kr.classList.contains('hidden')){
        now = 1;
    } else {
        now = 2;
    }

    console.log(now + ':now' + ' change Language');
    // console.log(jp.innerHTML + ': jp ' + kr.innerHTML + ' : kr');

    if(now == 0){
        kr.classList.add('hidden');
        now = 1;
        langChange.innerHTML = ' 한 / <strong>일 </strong>';
    }else if(now == 1){
        jp.classList.add('hidden');
        kr.classList.remove('hidden');
        now = 2;
        langChange.innerHTML = '<strong> 한</strong> / 일 ';
    } else { // now == 2;
        jp.classList.remove('hidden');
        now = 0;
        langChange.innerHTML = '<strong> 한 + 일 </strong>';
    }

}