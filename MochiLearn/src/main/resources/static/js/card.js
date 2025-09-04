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

$(document).ready(function() {
    const urlParams = new URLSearchParams(window.location.search);
    const cardId = urlParams.get('cardId');

    
	//나증에 멤버아이디 받아오는걸루 수정하기~
    const memberId = 1; // 로그인 세션 등에서 받아와야 함

    console.log("🏷️ URL에서 추출한 cardId:", cardId);

	function updateLikeButton() {
	        $('#cardLike').text(likeCount);
	        if (liked) {
	            $('#like-button').css('color', 'red');
	        } else {
	            $('#like-button').css('color', 'black');
	        }
	    }

    if (cardId) {
        $.get(`/mochilearn/api/study/card?cardId=${cardId}`)
        .done(function(cardData) {
            console.log("✅ API 호출 성공, 받은 card 데이터:", cardData);
			
			
            card = cardData;
            card.videoId = extractVideoId(card.url);
            renderCard(card);
            renderSectionButtons(card.sections);

			// 좋아요 수와 멤버 좋아요 상태 초기화
						liked = card.liked || false;
				        likeCount = card.like || 0;
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

        $('#like-button').click(function() {
            // 낙관적 업데이트
            liked = !liked;
            likeCount += liked ? 1 : -1;
            $('#cardLike').text(likeCount);
            updateLikeButton();

            // 서버에 좋아요 토글 요청
            $.post('/mochilearn/api/study/likes/toggle', { memberId: memberId, cardId: cardId })
            .done(function(response) {
                if ((response === 'liked' && !liked) || (response === 'unliked' && liked)) {
                    // 서버 상태와 UI 불일치 시 롤백
                    liked = !liked;
                    likeCount += liked ? 1 : -1;
                    $('#cardLike').text(likeCount);
                    updateLikeButton();
                }
            })
            .fail(function() {
                // 실패 시 롤백 및 알림
                liked = !liked;
                likeCount += liked ? 1 : -1;
                $('#cardLike').text(likeCount);
                updateLikeButton();
                alert('좋아요 처리 중 오류가 발생했습니다.');
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

const renderTimelines = () => {
    const timelineContainer = document.getElementById('timeline-buttons-container');
    timelineContainer.innerHTML = '';

    // timeline 배열 반복하면서 각 타임라인과 연동되는 버튼 생성 (버튼 클릭 시 재생 기능 등)
    timelines.forEach((timeline, index) => {
        const button = document.createElement('button');
        button.className = 'timeline-button';
        button.textContent = `타임라인 ${index + 1}`;
        // 버튼 클릭 시 타임라인 재생 함수 호출 (연결 기능 유지)
        button.onclick = () => startTimelinePlayback(timeline);

        timelineContainer.appendChild(button);
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

            // section 객체 내에 sentence 배열이 바로 있다고 가정
            const transcript = section.sentences || [];  // sentences 배열이 section 안에 있음

            startTimelinePlayback({
                start: section.start_seconds,
                end: section.end_seconds,
                transcript: transcript,
            });
        });

        container.appendChild(button);
    });
}


// 핵심 로직 함수 - 타임라인 재생, 자동정지, 자막 전환
const startTimelinePlayback = (timeline) => {
	console.log("▶ startTimelinePlayback 실행, timeline 데이터:", timeline);
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



