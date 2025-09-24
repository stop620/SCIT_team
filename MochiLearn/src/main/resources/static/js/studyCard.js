// 전역 변수
let player; // 유튜브 플레이어
let timelineTimeout;
let subtitleInterval;
const timelines = [];
let currentTranscript = [];
let currentTranscriptIndex = 0;
let liked = false;
let likeCount = 0;
let currentSectionStart = 0; // 현재 섹션 시작 시간
let currentSectionEnd = null; // 현재 섹션 종료 시간
let singleLineMode = false; // 한 문장만 재생 모드 여부

function renderCard(cardData) {
    document.getElementById('cardTitle').innerText = cardData.title || "";
    document.getElementById('cardLike').innerText = cardData.like || "";

    const tagsContainer = document.querySelector('.tags.card');
    tagsContainer.innerHTML = ''; // 초기화

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
        $('#like-button').css('color', liked ? '#f1c232' : 'black');
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

        $.get('/mochilearn/api/user/session')
            .done(function(userData) {
                isLoggedIn = !!(userData && userData.loggedIn);
            })
            .fail(function() {
                isLoggedIn = false;
            });

        $('#like-button').click(function() {
            if (!isLoggedIn) {
                if (confirm('좋아요를 누르려면 로그인해야 합니다. 로그인 페이지로 이동하시겠습니까?')) {
                    window.location.href = '/mochilearn/member/loginForm'; // 로그인 페이지 URL에 맞게 변경
                }
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

		$('#delete-button').click(function() {
		    if (!confirm("카드를 삭제하시겠습니까?")) return;

		    alert("카드가 삭제되었습니다.");  // 먼저 메시지 표시

		    // 페이지 이동 먼저 수행
		    window.location.href = "/mochilearn/page/study";

		    // 백그라운드에서 삭제 요청 (페이지 이동과 거의 동시에 실행됨)
		    $.ajax({
		        url: `/mochilearn/api/study/card/${cardId}`,
		        type: 'DELETE',
		        error: function() {
		            // 이동 후에는 사용자가 이 메시지를 보지 못할 수 있음
		            console.error("카드 삭제 처리 중 오류가 발생했습니다.");
		        }
		    });
		});


        $('#prev-btn').click(() => changeTranscriptIndex(-1));
        $('#next-btn').click(() => changeTranscriptIndex(1));

        // 단어선택, 말하기 기능 초기화 함수
        initializeWordSelectionEventListeners();
        initializeSpeechPracticeListeners();
    } else {
        console.warn("⚠️ URL에 cardId 파라미터가 존재하지 않음");
    }
});

// 시간 변환 함수
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
    if (event.data === YT.PlayerState.PLAYING) {
        if (subtitleInterval) clearInterval(subtitleInterval);
        subtitleInterval = setInterval(() => {
            if (!player || typeof player.getCurrentTime !== 'function') {
                clearInterval(subtitleInterval);
                return;
            }
            const currentTime = player.getCurrentTime();
            const relativeTime = currentTime - currentSectionStart;
            const lastIndex = currentTranscript.length - 1;
            const lastTime = lastIndex >= 0 ? parseAITime(currentTranscript[lastIndex].time) : 0;

            if (!singleLineMode) { // 한 문장 모드일 땐 인덱스 안바꿈
                let newIndex = -1;
                for (let i = 0; i < currentTranscript.length; i++) {
                    const itemTime = parseAITime(currentTranscript[i].time);
                    const nextItemTime = (i + 1 < currentTranscript.length) ? parseAITime(currentTranscript[i + 1].time) : Infinity;

                    if (relativeTime >= itemTime && relativeTime < nextItemTime) {
                        newIndex = i;
                        break;
                    }
                }

                if (newIndex === -1 && currentTranscript.length > 0 && relativeTime >= lastTime) {
                    newIndex = lastIndex;
                }

                if (newIndex !== currentTranscriptIndex && newIndex !== -1) {
                    currentTranscriptIndex = newIndex;
                    updateSingleTranscriptLine();
                }

                if (currentTime >= currentSectionStart + lastTime + 0.5) {
                    clearInterval(subtitleInterval);
                    return;
                }
            }
        }, 100);
    } else if (event.data === YT.PlayerState.PAUSED || event.data === YT.PlayerState.ENDED) {
        clearInterval(subtitleInterval);
        clearInterval(timelineTimeout);
    }
};

function updateSingleTranscriptLine() {
    const transcriptContainer = $('#transcript-container');
    const japaneseLineElement = $('#japanese-line');

    transcriptContainer.toggleClass('hidden', currentTranscript.length === 0);
    japaneseLineElement.empty();

    if (currentTranscript.length > 0) {
        const item = currentTranscript[currentTranscriptIndex];

        if (item.japaneseTokens && Array.isArray(item.japaneseTokens)) {
            item.japaneseTokens.forEach((token, index) => {
                const span = $('<span></span>', {
                    text: token.surface,
                    class: `japanese-token ${isActionableToken(token) ? 'actionable' : 'non-actionable'}`,
                    'data-index': index
                }).prop('tokenData', token);
                japaneseLineElement.append(span);
            });
        } else {
            japaneseLineElement.text(item.japanese);
        }

        $('#korean-line').text(item.korean);
        $('#transcript-index').text(`${currentTranscriptIndex + 1} / ${currentTranscript.length}`);
    }
}

const changeTranscriptIndex = (direction) => {
    if (currentTranscript.length === 0) return;

    if (direction === -1) {
        if (currentTranscriptIndex === 0) return;
        currentTranscriptIndex -= 1;
    } else if (direction === 1) {
        if (currentTranscriptIndex === currentTranscript.length - 1) return;
        currentTranscriptIndex += 1;
    } else {
        return;
    }

    updateSingleTranscriptLine();

    if (player && typeof player.seekTo === 'function') {
        const relativeTime = parseAITime(currentTranscript[currentTranscriptIndex].time);
        const absoluteTime = currentSectionStart + relativeTime;
        player.seekTo(absoluteTime, true);
        player.playVideo();

        // 자막 버튼으로 재생 시에도 종료 시간 체크 타이머 설정
        if (currentSectionEnd !== null) {
            clearInterval(timelineTimeout);
            timelineTimeout = setInterval(() => {
                if (player && typeof player.getCurrentTime === 'function') {
                    if (player.getCurrentTime() >= currentSectionEnd) {
                        player.pauseVideo();
                        clearInterval(timelineTimeout);
                    }
                }
            }, 100);
        }
    }
};

function renderSectionButtons(sections) {
    const container = document.getElementById('timeline-buttons-container');
    container.innerHTML = '';

    sections.forEach(section => {
        console.log(section);
        const wrapper = document.createElement('div');
        wrapper.className = 'timeline-button-wrapper';

        const button = document.createElement('button');
        button.className = 'timeline-button';
        button.textContent = section.section_num || section.sectionNum || "구간";
        button.setAttribute('data-section-id', section.id);

        const timeInfo = document.createElement('span');
        timeInfo.className = 'selected-section-display';
        timeInfo.innerHTML = `- ${time(section.start_seconds)}~${time(section.end_seconds)}`;

        const sample = document.createElement('div');
        sample.className = 'timeline-sample';

        let sampleJp;
        let sampleKr;
        if (section.sentences[0].japanese.length < 5) {
            sampleJp = section.sentences[1].japanese;
            sampleKr = section.sentences[1].korean;
        } else {
            sampleJp = section.sentences[0].japanese;
            sampleKr = section.sentences[0].korean;
        }

        sample.innerHTML = `<span>${sampleJp}</span><span>${sampleKr}</span>`;

        button.addEventListener('click', () => {
            console.log(`🎯 섹션 버튼 클릭 - ID: ${section.id}, 번호: ${section.section_num || section.sectionNum}`);

            currentSectionStart = section.start_seconds || 0;
            currentSectionEnd = section.end_seconds || null;
            currentTranscript = section.sentences || [];
            currentTranscriptIndex = 0;
            updateSingleTranscriptLine();

            startTimelinePlayback({
                start: currentSectionStart,
                end: currentSectionEnd,
                transcript: currentTranscript,
            });
        });

        wrapper.appendChild(button);
        wrapper.appendChild(sample);
        wrapper.appendChild(timeInfo);

        container.appendChild(wrapper);
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
            const relativeTime = currentTime - timeline.start;

            let newIndex = -1;
            for (let i = 0; i < currentTranscript.length; i++) {
                const itemTime = parseAITime(currentTranscript[i].time);
                const nextItemTime = (i + 1 < currentTranscript.length) ? parseAITime(currentTranscript[i + 1].time) : Infinity;

                if (relativeTime >= itemTime && relativeTime < nextItemTime) {
                    newIndex = i;
                    break;
                }
            }
            if (newIndex === -1 && currentTranscript.length > 0 && relativeTime >= parseAITime(currentTranscript[currentTranscript.length - 1].time)) {
                newIndex = currentTranscript.length - 1;
            }

            if (newIndex !== currentTranscriptIndex && newIndex !== -1) {
                currentTranscriptIndex = newIndex;
                updateSingleTranscriptLine();
            }

            if (currentTime >= timeline.end) {
                player.pauseVideo();
                clearInterval(subtitleInterval);
                return;
            }
        }, 100);
    }
};

function time(seconds) {

    //3항 연산자를 이용하여 10보다 작을 경우 0을 붙이도록 처리 하였다.
    let hour = parseInt(seconds/3600) < 10 ? '0'+ parseInt(seconds/3600) : parseInt(seconds/3600);
    let min = parseInt((seconds%3600)/60) < 10 ? '0'+ parseInt((seconds%3600)/60) : parseInt((seconds%3600)/60);
    let sec = seconds % 60 < 10 ? '0'+seconds % 60 : seconds % 60;

    if(hour == '00') {
        return min+":" + sec;
    } else {
        return hour+":"+min+":" + sec;
    }

}

// 문장 단위 재생 버튼 이벤트
$(document).on('click', '.play-script-btn', function() {
    if (!player || currentTranscript.length === 0) return;

    const startTime = currentSectionStart + parseAITime(currentTranscript[currentTranscriptIndex].time);
    const endTime = (currentTranscriptIndex + 1 < currentTranscript.length)
        ? currentSectionStart + parseAITime(currentTranscript[currentTranscriptIndex + 1].time)
        : (currentSectionEnd !== null ? currentSectionEnd : player.getDuration());

    // 모드 켜기
    singleLineMode = true;

    // 영상 재생
    player.seekTo(startTime, true);
    player.playVideo();

    // 기존 타이머 정리
    clearInterval(timelineTimeout);

    // 종료 지점에서 멈춤
    timelineTimeout = setInterval(() => {
        if (player && typeof player.getCurrentTime === 'function') {
            if (player.getCurrentTime() >= endTime) {
                player.pauseVideo();
                clearInterval(timelineTimeout);
                singleLineMode = false; // 정지 후 다시 원래 모드로
            }
        }
    }, 100);
});