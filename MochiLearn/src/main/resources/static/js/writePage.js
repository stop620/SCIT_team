// 전역 변수
let player; // 유튜브 플레이어
let timelineTimeout;
const timelines = [];
let quiz = [];
let currentTranscript = [];
let currentTranscriptIndex = 0;
let subtitleInterval;

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
    // 일반 영상과 쇼츠 영상 URL을 모두 처리하도록 정규식 수정
    const regex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/|youtube\.com\/shorts\/)([^"&?\/\s]{11})/i;
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

        const timeInfo = document.createElement('span');
        timeInfo.className = 'selected-section-display';
        timeInfo.innerHTML = `- ${timeline.label}`;
        const sample = document.createElement('div');
        sample.className = 'timeline-sample';

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

                let sampleJp;
                let sampleKr;
                if (timeline.transcript[0].japanese.length < 5) {
                    sampleJp = timeline.transcript[1].japanese;
                    sampleKr = timeline.transcript[1].korean;
                } else {
                    sampleJp = timeline.transcript[0].japanese;
                    sampleKr = timeline.transcript[0].korean;
                }

                sample.innerHTML = `<span>${sampleJp}</span><span>${sampleKr}</span>`;
            }
        }
        wrapper.appendChild(button);
        wrapper.appendChild(timeInfo);
        wrapper.insertBefore(sample, wrapper.lastElementChild);

        timelineContainer.appendChild(wrapper);
    });
};

// 핵심 로직 함수 - 타임라인 재생, 자동정지, 자막 전환
const startTimelinePlayback = (timeline) => {
    document.getElementById('selected-section-display').textContent = `- ${timeline.label}`;

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
        console.log('currentTranscript: ' + currentTranscript);
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

// 자막 요청 후 받아올때까지 지속적으로 결과 확인
const pollForResult = (jobId) => {
    fetch(`/mochilearn/api/transcribe/status/${jobId}`)
        .then(response => response.ok ? response.json() : Promise.reject('상태 확인 중 서버 오류 발생'))
        .then(statusData => {
            const targetTimeline = timelines.find(t => t.id === jobId);
            if (!targetTimeline) return;

            if (statusData.status === 'PROCESSING') { // 서버가 작업 진행중
                setTimeout(() => pollForResult(jobId), 2000);
            } else if (statusData.status === 'COMPLETED') { // 완료된 결과 받은 경우
                targetTimeline.status = 'COMPLETED';
                targetTimeline.transcript = statusData.result.sentences;

                console.log(statusData.result);
                console.log(targetTimeline.transcript);

                if (statusData.result && statusData.result.quizSentences) {
                    quiz = quiz.concat(statusData.result.quizSentences);
                    console.log(quiz);
                }

                console.log("Updated timelines:", timelines);
                console.log("Updated quiz array:", quiz);

                updateLevelButton(statusData.result.level);
                renderTimelines();

            } else if (statusData.status === 'FAILED') { // 실패
                targetTimeline.status = 'FAILED';
                renderTimelines();
            }
        })
        .catch(error => {
            console.error('Error during polling:', error);
            const failedTimeline = timelines.find(t => t.id === jobId);
            if (failedTimeline) {
                failedTimeline.status = 'FAILED';
                renderTimelines();
            }
        });
};

// 태그 컨트롤 함수
const setupTagSelection = () => {
    const difficultyContainer = document.getElementById('difficulty-tags');
    const genreContainer = document.getElementById('genre-tags');

    difficultyContainer.addEventListener('click', (e) => {
        if (e.target.classList.contains('tag-button')) {
            difficultyContainer.querySelectorAll('.tag-button').forEach(btn => btn.classList.remove('selected'));
            e.target.classList.add('selected');
        }
    });

    genreContainer.addEventListener('click', (e) => {
        if (e.target.classList.contains('tag-button')) {
            e.target.classList.toggle('selected');
        }
    });
};

// 로드 시 페이지 초기화
document.addEventListener('DOMContentLoaded', () => {
    const urlInput = document.getElementById('url-input');
    const addTimelineBtn = document.getElementById('add-timeline-btn');
    const startInput = document.getElementById('start-input');
    const endInput = document.getElementById('end-input');
    const saveCardBtn = document.getElementById('save-card-btn');
    // --- '유튜브 제목 사용' 버튼 요소 가져오기 ---
    const useYtTitleBtn = document.getElementById('use-yt-title-btn');

    const levelTagInfo = document.querySelector('.level-tag-div svg');
    const tooltip = document.getElementById("tooltip");

    levelTagInfo.addEventListener('mouseenter', () => {
        console.log('over');
        tooltip.classList.add("show");
    });
    tooltip.addEventListener('mouseleave', () => {
       console.log('out');
        tooltip.classList.remove("show");
    });

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
                start, end,
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
                    if (optimisticTimeline) optimisticTimeline.id = result.jobId;
                    pollForResult(result.jobId);
                })
                .catch(error => {
                    alert('작업 요청 중 오류가 발생했습니다: ' + error.message);
                    const failedIndex = timelines.findIndex(t => t.id === tempId);
                    if (failedIndex > -1) timelines.splice(failedIndex, 1);
                    renderTimelines();
                });
        } else {
            alert('유효한 유튜브 URL과 타임라인 구간(시작 시간 < 종료 시간)을 입력해주세요.');
        }
    });

    document.getElementById('prev-btn').addEventListener('click', () => changeTranscriptIndex(-1));
    document.getElementById('next-btn').addEventListener('click', () => changeTranscriptIndex(1));

    // 유튜브 제목 사용 버튼 이벤트 리스너 추가
    if (useYtTitleBtn) {
        useYtTitleBtn.addEventListener('click', () => {
            if (player && typeof player.getVideoData === 'function' && player.getVideoData().title) {
                const videoTitle = player.getVideoData().title;
                document.getElementById('title-input').value = videoTitle;
            } else {
                alert('먼저 유효한 YouTube 영상을 로드해주세요.');
            }
        });
    }

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
                sentences: t.transcript
            }));

        if (!title || !url || !level || tag.length === 0 || completedSections.length === 0) {
            alert('제목, URL, 난이도, 장르를 모두 선택하고, 하나 이상의 학습 구간을 추가해주세요.');
            return;
        }
        console.log(completedSections);

        const saveData = {
                "card": {
                    title: title,
                    url: url,
                    level: level,
                    tag: tag,
                    sections: completedSections,
                },
                "quiz": quiz

            };

        console.log('Saving Card Data:', JSON.stringify(saveData, null, 2));

        fetch('/mochilearn/api/study/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(saveData)
        })
            .then(response => response.ok ? response.json() : Promise.reject(response.json()))
            .then(data => {
                alert('학습 카드가 성공적으로 저장되었습니다!');
                window.location.href = '/mochilearn/page/study';
            })
            .catch(errorPromise => {
                errorPromise.then(err => {
                    alert('저장 중 오류가 발생했습니다: ' + (err.message || '알 수 없는 오류'));
                }).catch(() => {
                    alert('저장 중 심각한 오류가 발생했습니다.');
                });
                console.error('Save Error:', errorPromise);
            });
    });
});

function updateLevelButton(level) {

    const levelContainer = document.getElementById('difficulty-tags');
    let levelButton;

    switch (level) {
        case 1:
            levelButton = levelContainer.children.item(0);
            console.log(levelButton);
            break;
        case 2:
            levelButton = levelContainer.children.item(1);
            console.log(levelButton);
            break;
        case 3:
            levelButton = levelContainer.children.item(2);
            console.log(levelButton);
            break;
        default:
            break;
    }
    levelContainer.querySelectorAll('.tag-button').forEach(btn => btn.classList.remove('selected'));
    levelButton.classList.add('selected');
}