// 전역 변수
let player;
let timelineTimeout;
const timelines = [];
let currentTranscript = [];
let currentTranscriptIndex = 0;
let subtitleInterval;

// --- 시간 변환 헬퍼 함수 ---
const parseTime = (timeString) => {
    const parts = timeString.split(':').map(Number);
    return parts.length === 2 ? parts[0] * 60 + parts[1] : 0;
};

const formatTime = (seconds) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.floor(seconds % 60);
    return `${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`;
};

// --- YouTube 관련 함수 ---
const extractVideoId = (url) => {
    const regex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})/i;
    const match = url.match(regex);
    return match ? match[1] : null;
};

function onYouTubeIframeAPIReady() {}

const createPlayer = (videoId) => {
    if (player) player.destroy();
    player = new YT.Player('player', {
        height: '100%',
        width: '100%',
        videoId: videoId,
        playerVars: { 'playsinline': 1, 'controls': 1, 'rel': 0 },
        events: {
            'onStateChange': onPlayerStateChange
        }
    });
};

const onPlayerStateChange = (event) => {
    if (event.data === YT.PlayerState.PAUSED || event.data === YT.PlayerState.ENDED) {
        clearInterval(subtitleInterval);
    }
};

// --- UI 렌더링 함수 ---
const updateSingleTranscriptLine = () => {
    const transcriptContainer = document.getElementById('transcript-container');
    transcriptContainer.classList.toggle('hidden', currentTranscript.length === 0);

    if (currentTranscript.length > 0) {
        const item = currentTranscript[currentTranscriptIndex];
        document.getElementById('japanese-line').textContent = item.japanese;
        document.getElementById('korean-line').textContent = item.korean;
        document.getElementById('transcript-index').textContent = `${currentTranscriptIndex + 1} / ${currentTranscript.length}`;
    }
};

const changeTranscriptIndex = (direction) => {
    if (currentTranscript.length === 0) return;
    currentTranscriptIndex = (currentTranscriptIndex + direction + currentTranscript.length) % currentTranscript.length;
    updateSingleTranscriptLine();
};

const renderTimelines = () => {
    const timelineContainer = document.getElementById('timeline-buttons-container');
    timelineContainer.innerHTML = '';
    timelines.forEach((timeline, index) => {
        const button = document.createElement('button');
        button.className = 'timeline-button'; // 기본 클래스
        button.setAttribute('aria-label', timeline.label);

        if (timeline.status === 'PROCESSING') {
            button.disabled = true;
            button.classList.add('loading'); // 로딩 클래스 추가
            button.innerHTML = `<svg class="animate-spin h-6 w-6 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>`;
        } else if (timeline.status === 'FAILED') {
            button.disabled = true;
            button.classList.add('failed'); // 실패 클래스 추가
            button.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-white"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`;
        } else { // COMPLETED
            button.classList.add('completed'); // 완료 클래스 추가
            button.innerHTML = `<span class="timeline-button-label">${index + 1}</span>`;
            button.onclick = () => startTimelinePlayback(timeline);
        }
        timelineContainer.appendChild(button);
    });
};

// --- 핵심 로직 함수 ---
const startTimelinePlayback = (timeline) => {
    document.getElementById('selected-section-display').textContent = `- ${timeline.label}`;

    if (player) {
        player.seekTo(timeline.start, true);
        player.playVideo();

        clearTimeout(timelineTimeout);
        clearInterval(subtitleInterval);

        timelineTimeout = setTimeout(() => player.pauseVideo(), (timeline.end - timeline.start) * 1000);

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

            let newIndex = -1;
            for (let i = 0; i < currentTranscript.length; i++) {
                const itemTime = parseFloat(currentTranscript[i].time);
                if (relativeTime >= itemTime) {
                    newIndex = i;
                } else {
                    break;
                }
            }

            if (newIndex !== -1 && newIndex !== currentTranscriptIndex) {
                currentTranscriptIndex = newIndex;
                updateSingleTranscriptLine();
            }
        }, 250);
    }
};

const pollForResult = (jobId) => {
    fetch(`/mochilearn/api/transcribe/status/${jobId}`)
        .then(response => response.ok ? response.json() : Promise.reject('상태 확인 중 서버 오류 발생'))
        .then(statusData => {
            const targetTimeline = timelines.find(t => t.id === jobId);
            if (!targetTimeline) return;

            if (statusData.status === 'PROCESSING') {
                setTimeout(() => pollForResult(jobId), 2000);
            } else if (statusData.status === 'COMPLETED') {
                targetTimeline.status = 'COMPLETED';
                targetTimeline.transcript = statusData.result;
                renderTimelines();
            } else if (statusData.status === 'FAILED') {
                targetTimeline.status = 'FAILED';
                renderTimelines();
                throw new Error(statusData.error || '알 수 없는 오류로 작업에 실패했습니다.');
            }
        })
        .catch(error => {
            alert('대사 추출 중 오류가 발생했습니다: ' + error.message);
            console.error('Error:', error);
            const failedTimeline = timelines.find(t => t.id === jobId);
            if (failedTimeline) {
                failedTimeline.status = 'FAILED';
                renderTimelines();
            }
        });
};

const setupTagSelection = () => {
    const difficultyContainer = document.getElementById('difficulty-tags');
    const genreContainer = document.getElementById('genre-tags');

    // 난이도 태그 (단일 선택)
    difficultyContainer.addEventListener('click', (e) => {
        if (e.target.classList.contains('tag-button')) {
            difficultyContainer.querySelectorAll('.tag-button').forEach(btn => {
                btn.classList.remove('selected');
            });
            e.target.classList.add('selected');
        }
    });

    // 장르 태그 (다중 선택)
    genreContainer.addEventListener('click', (e) => {
        if (e.target.classList.contains('tag-button')) {
            e.target.classList.toggle('selected');
        }
    });
};

// --- 페이지 초기화 ---
window.onload = () => {
    const urlInput = document.getElementById('url-input');
    const addTimelineBtn = document.getElementById('add-timeline-btn');
    const startInput = document.getElementById('start-input');
    const endInput = document.getElementById('end-input');
    const saveCardBtn = document.getElementById('save-card-btn');

    setupTagSelection();

    urlInput.addEventListener('input', () => {
        const url = urlInput.value;
        const videoId = extractVideoId(url);
        document.getElementById('player-container').classList.toggle('hidden', !videoId);
        document.getElementById('selected-section-display').textContent = '';
        if (videoId) createPlayer(videoId);
        else if (player) { player.destroy(); player = null; }
    });

    addTimelineBtn.addEventListener('click', () => {
        const url = urlInput.value;
        const start = parseTime(startInput.value);
        const end = parseTime(endInput.value);

        if (url && start >= 0 && end > start) {
            const data = { url, start, end };

            const tempId = `temp_${Date.now()}`;
            const newTimeline = {
                id: tempId,
                start,
                end,
                label: `${startInput.value}~${endInput.value}`,
                status: 'PROCESSING',
                transcript: []
            };
            timelines.push(newTimeline);
            renderTimelines();

            startInput.value = '';
            endInput.value = '';

            fetch('/mochilearn/api/transcribe/start', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            })
                .then(response => response.ok ? response.json() : Promise.reject('서버 응답 오류: ' + response.statusText))
                .then(result => {
                    const optimisticTimeline = timelines.find(t => t.id === tempId);
                    if (optimisticTimeline) {
                        optimisticTimeline.id = result.jobId;
                    }
                    pollForResult(result.jobId);
                })
                .catch(error => {
                    alert('작업 요청 중 오류가 발생했습니다: ' + error.message);
                    console.error('Error:', error);
                    const failedIndex = timelines.findIndex(t => t.id === tempId);
                    if (failedIndex > -1) {
                        timelines.splice(failedIndex, 1);
                    }
                    renderTimelines();
                });
        } else {
            alert('유효한 유튜브 URL과 타임라인 구간(시작 시간 < 종료 시간)을 입력해주세요.');
        }
    });

    document.getElementById('prev-btn').addEventListener('click', () => changeTranscriptIndex(-1));
    document.getElementById('next-btn').addEventListener('click', () => changeTranscriptIndex(1));

    saveCardBtn.addEventListener('click', () => {
        const title = document.getElementById('title-input').value;
        const url = document.getElementById('url-input').value;

        const selectedDifficultyEl = document.querySelector('#difficulty-tags .selected');
        const level = selectedDifficultyEl ? selectedDifficultyEl.textContent : null;

        const selectedTagEls = document.querySelectorAll('#genre-tags .selected');
        const tag = Array.from(selectedTagEls).map(el => el.textContent).join(',');

        const completedSections = timelines
            .filter(t => t.status === 'COMPLETED')
            .map((t, index) => ({
                section_num: index + 1,
                start_seconds: t.start,
                end_seconds: t.end,
                sentences: t.transcript.map(s => ({
                    index: s.index,
                    time: s.time,
                    japanese: s.Japanese,
                    korean: s.Korean
                }))
            }));

        if (!title || !url || !level || tag.length === 0 || completedSections.length === 0) {
            alert('제목, URL, 난이도, 장르를 모두 선택하고, 하나 이상의 학습 구간을 추가해주세요.');
            return;
        }

        const cardData = {
            title: title,
            url: url,
            level: level,
            tag: tag,
            sections: completedSections
        };

        console.log('Saving Card Data:', JSON.stringify(cardData, null, 2));

        fetch('/mochilearn/api/study/save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(cardData)
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => { throw new Error(err.message || '저장에 실패했습니다.') });
                }
                return response.json();
            })
            .then(data => {
                alert('학습 카드가 성공적으로 저장되었습니다!');
                window.location.href = '/mochilearn/page/study';
            })
            .catch(error => {
                console.error('Save Error:', error);
                alert('저장 중 오류가 발생했습니다: ' + error.message);
            });
    });
};