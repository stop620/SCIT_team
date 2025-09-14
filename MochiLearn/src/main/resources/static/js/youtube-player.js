
// YouTube IFrame API를 사용하여 플레이어를 제어하고,구간 재생 및 자막 동기화 관리

// 전역 변수
let timelineTimeout;
let subtitleInterval;

/**
 * YouTube IFrame API가 준비되면 호출되는 콜백 함수
 */
function onYouTubeIframeAPIReady() {
    if (card && card.videoId) {
        createPlayer(card.videoId);
    }
}

/**
 * YouTube 플레이어 인스턴스를 생성하는 함수
 * @param {string} videoId - 재생할 YouTube 영상 ID
 */
function createPlayer(videoId) {
    if (player) player.destroy();
    player = new YT.Player('player', {
        height: '100%',
        width: '100%',
        videoId: videoId,
        playerVars: { 'playsinline': 1, 'controls': 1, 'rel': 0 },
        events: { 'onStateChange': onPlayerStateChange }
    });
}

/**
 * 플레이어 상태 변경 시 호출되는 이벤트 핸들러
 */
function onPlayerStateChange(event) {
    if (event.data === YT.PlayerState.PAUSED || event.data === YT.PlayerState.ENDED) {
        clearInterval(subtitleInterval);
        clearInterval(timelineTimeout);
    }
}

/**
 * 학습 구간 버튼들을 UI에 렌더링하는 함수
 */
function renderSectionButtons(sections) {
    const container = $('#timeline-buttons-container');
    container.empty();
    sections.forEach(section => {
        const button = $('<button></button>', {
            class: 'timeline-button',
            text: section.section_num || section.sectionNum || "구간",
            click: () => {
                currentSectionStart = section.start_seconds || 0;
                startTimelinePlayback({
                    start: currentSectionStart,
                    end: section.end_seconds,
                    transcript: section.sentences || [],
                });
            }
        });
        container.append(button);
    });
}

/**
 * 특정 구간의 재생을 시작하는 함수
 * @param {object} timeline - 시작 시간, 종료 시간, 자막 데이터를 포함한 객체
 */
function startTimelinePlayback(timeline) {
    if (!player) return;

    player.seekTo(timeline.start, true);
    player.playVideo();

    clearInterval(timelineTimeout);
    clearInterval(subtitleInterval);

    // 타임라인 종료 구간 도달 시 자동 정지
    timelineTimeout = setInterval(() => {
        if (player?.getCurrentTime() >= timeline.end) {
            player.pauseVideo();
            clearInterval(timelineTimeout);
        }
    }, 100);

    // 자막 데이터 초기화
    currentTranscript = timeline.transcript;
    currentTranscriptIndex = 0;
    updateSingleTranscriptLine();

    // 자막 동기화 시작
    syncSubtitles(timeline.start, timeline.end);
}

/**
 * 영상 재생 시간에 맞춰 자막을 동기화하는 함수
 */
function syncSubtitles(sectionStart, sectionEnd) {
    subtitleInterval = setInterval(() => {
        if (!player?.getCurrentTime) return;

        const currentTime = player.getCurrentTime();
        if (currentTime < sectionStart || currentTime > sectionEnd) return;

        const relativeTime = currentTime - sectionStart;
        let newIndex = -1;

        for (let i = 0; i < currentTranscript.length; i++) {
            const itemTime = parseAITime(currentTranscript[i].time);
            const nextItemTime = (i + 1 < currentTranscript.length) ? parseAITime(currentTranscript[i + 1].time) : Infinity;
            if (relativeTime >= itemTime && relativeTime < nextItemTime) {
                newIndex = i;
                break;
            }
        }

        if (newIndex !== -1 && newIndex !== currentTranscriptIndex) {
            currentTranscriptIndex = newIndex;
            updateSingleTranscriptLine();
        }
    }, 100);
}


// --- 헬퍼 함수 ---
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
