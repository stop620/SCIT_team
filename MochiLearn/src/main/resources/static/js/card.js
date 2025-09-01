// 전역 변수
let player; // 유튜브 플레이어
let timelineTimeout;
const timelines = [];
let currentTranscript = [];
let currentTranscriptIndex = 0;
let subtitleInterval;

function renderCard(cardData) {
    document.getElementById('cardTitle').innerText = cardData.title || "";
    document.getElementById('cardLike').innerText = cardData.like || "";
}

/*$(document).ready(function() {
    const urlParams = new URLSearchParams(window.location.search);
    const cardId = urlParams.get('cardId');

    console.log("🏷️ URL에서 추출한 cardId:", cardId);

    if (cardId) {
        $.get(`/mochilearn/api/study/card?cardId=${cardId}`)
        .done(function(cardData) {
            console.log("✅ API 호출 성공, 받은 card 데이터:", cardData);
            card = cardData;
            card.videoId = extractVideoId(card.url);
            renderCard(card);
            renderSectionButtons(card.sections);

            if (typeof YT !== 'undefined' && YT && YT.Player) {
                createPlayer(card.videoId);
            } else {
                console.warn("⚠️ YouTube IFrame API가 아직 로드되지 않음");
            }
        })
        .fail(function(jqXHR, textStatus, errorThrown) {
            console.error("❌ API 호출 실패:", textStatus, errorThrown);
        });
    } else {
        console.warn("⚠️ URL에 cardId 파라미터가 존재하지 않음");
    }
});*/

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

// YouTube 관련 함수
const extractVideoId = (url) => {
    const regex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})/i;
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
    // 영상이 정지되거나 끝나면, 모든 자동화 타이머(자막, 구간정지)를 중단합니다.
    if (event.data === YT.PlayerState.PAUSED || event.data === YT.PlayerState.ENDED) {
        clearInterval(subtitleInterval);
        clearInterval(timelineTimeout);
    }
};

// UI 렌더링 함수
const updateSingleTranscriptLine = () => {
    const transcriptContainer = document.getElementById('transcript-container');
    transcriptContainer.classList.toggle('hidden', currentTranscript.length === 0);
	console.log(currentTranscript.length)
    if (currentTranscript.length > 0) {
        const item = currentTranscript[currentTranscriptIndex];
        document.getElementById('japanese-line').textContent = item.japanese;
        document.getElementById('korean-line').textContent = item.korean;
        document.getElementById('transcript-index').textContent = `${currentTranscriptIndex + 1} / ${currentTranscript.length}`;
    }
};

// 자막 인덱스 관련
const changeTranscriptIndex = (direction) => {
    if (currentTranscript.length === 0) return;
    currentTranscriptIndex = (currentTranscriptIndex + direction + currentTranscript.length) % currentTranscript.length;
    updateSingleTranscriptLine();
};

const handleDeleteTimeline = (indexToDelete) => {
    timelines.splice(indexToDelete, 1);
    renderTimelines();
};

// 타임라인 버튼 생성
const renderTimelines = () => {
    const timelineContainer = document.getElementById('timeline-buttons-container');
    timelineContainer.innerHTML = '';
    timelines.forEach((timeline, index) => {
        const wrapper = document.createElement('div');
        wrapper.className = 'timeline-button-wrapper';

        const button = document.createElement('button');
        button.className = 'timeline-button';
        button.setAttribute('aria-label', timeline.label);

        button.classList.remove('loading', 'failed', 'completed');

        if (timeline.status === 'PROCESSING') {
            button.disabled = true;
            button.classList.add('loading');
            button.innerHTML = `<svg class="animate-spin h-6 w-6 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>`;
        } else {
            button.innerHTML = `<span class="timeline-button-label">${index + 1}</span>`;
            const deleteButton = document.createElement('button');
            deleteButton.className = 'timeline-delete-button';
            deleteButton.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>`;
            deleteButton.onclick = (e) => {
                e.stopPropagation();
                handleDeleteTimeline(index);
            };
            wrapper.appendChild(deleteButton);

            if (timeline.status === 'FAILED') {
                button.disabled = true;
                button.classList.add('failed');
            } else { // COMPLETED
                button.classList.add('completed');
                button.onclick = () => startTimelinePlayback(timeline);
            }
        }
        wrapper.appendChild(button);
        timelineContainer.appendChild(wrapper);
    });
};

function renderSectionButtons(sections) {
    const container = document.getElementById('timeline-buttons-container');
    container.innerHTML = '';  // 초기화

    sections.forEach(section => {
        const button = document.createElement('button');
        button.className = 'timeline-button';
        button.textContent = section.section_num || section.sectionNum || "구간";
        button.setAttribute('data-section-id', section.id);

        button.addEventListener('click', () => {
            console.log(`🎯 섹션 버튼 클릭 - ID: ${section.id}, 번호: ${section.section_num || section.sectionNum}`);
            startTimelinePlayback(section);
        });

        container.appendChild(button);
    });
}

// 핵심 로직 함수 - 타임라인 재생, 자동정지, 자막 전환
const startTimelinePlayback = (timeline) => {

    if (player) {
        player.seekTo(timeline.start, true);
        player.playVideo();

        clearInterval(timelineTimeout);
        clearInterval(subtitleInterval);

        // 타임라인 종료구간 도달 시 정지
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

        // 재생과 자막 싱크
        subtitleInterval = setInterval(() => {
            if (!player || typeof player.getCurrentTime !== 'function') {
                clearInterval(subtitleInterval); return;
            }
            const currentTime = player.getCurrentTime();
            if (currentTime < timeline.start || currentTime > timeline.end) {
                clearInterval(subtitleInterval); return;
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



