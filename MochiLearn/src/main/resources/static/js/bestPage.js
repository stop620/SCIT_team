console.log('best page loaded');
let cards = [];

$(document).ready(function() {
    console.log('document ready');

    // 로딩 스피너를 숨기는 함수
    function hideLoading(flag) {
        if(flag === 'popular') {
            $('#popularGrid .loading-spinner').remove();
        } else {
            $('#latestGrid .loading-spinner').remove();
        }
    }

    function loadCards(flag) {

        if(flag != null) {
            console.log('get cards');

            $.get('/mochilearn/api/study/load', {
                sort: flag,
                size: 4

            }).done(function(data) {
                // 로딩 스피너 숨기기 전에 기존 내용 지우기
                if(flag === 'popular') {
                    $('#popularGrid').empty();
                } else {
                    $('#latestGrid').empty();
                }

                if (!data || data.length === 0) {
                    console.log('결과가 없습니다.');
                    return;
                }
                renderCards(data, flag)
                console.log(data);

            }).fail(function() {
                console.log('카드 로드 중 오류가 발생했습니다.');
            }).always(function() {
                // 성공 또는 실패 여부와 관계없이 로딩 스피너 숨기기
                hideLoading(flag);
            });
        }
    }

    // 카드 렌더링 함수
    function renderCards(cards, flag) {
        console.log('render cards: flag=' + flag);

        cards.forEach(card => {
            const thumbUrl = getYoutubeThumbnail(card.url);
            let difficulty;
            let levelKor;
            let tagList = card.tag.split(',');
            switch (card.level) {
                case '1':
                    difficulty = 'beginner';
                    levelKor = '초급';
                    break;
                case '2':
                    difficulty = 'intermediate';
                    levelKor = '중급';
                    break;
                case '3':
                    difficulty = 'advanced';
                    levelKor = '고급';
                    break;
                default:
                    break;
            }

            let cardHtml = `
                    <div class="card" onclick="location.href='/mochilearn/page/studyCard?cardId=${card.id}'">
                        <div class="card-thumbnail">
                            <!-- 썸네일 공간 -->
                            <img class="thumbnail-image" src="${thumbUrl}" alt="썸네일 이미지" />
                            <div class="play-icon">▶</div>
                        </div>
                        <div class="video-info">
                            <h3 class="video-title">${card.title}</h3>
                            <div class="video-meta">
                                <span class="difficulty-tag ${difficulty}">${levelKor}</span>
                                <div class="video-stats">
                                    <span class="star">⭐</span> ${card.like}
                                </div>
                            </div>
                            <div class="tag-container">
                            `;

            tagList.forEach(tag => {
                cardHtml += `<span class="genre-tag-item">${tag}</span>`;
            });
            cardHtml += `</div>
                        </div>
                    </div>`;

            if(flag === 'popular') {
                $('#popularGrid').append(cardHtml);

            } else {
                $('#latestGrid').append(cardHtml);

            }

        });
    }

    // 유튜브 썸네일 추출 함수
    function getYoutubeThumbnail(youtubeUrl) {		// 쇼츠 처리 가능하게 수정
        let videoId = '';
        if (!youtubeUrl) {
            return '/images/default-thumbnail.png';  // URL이 없으면 기본 이미지 반환
        }

        // 쇼츠 URL (youtube.com/shorts/) 처리
        if (youtubeUrl.includes('youtube.com/shorts/')) {
            videoId = youtubeUrl.split('youtube.com/shorts/')[1].split(/[?&]/)[0];
        }
        // 짧은 URL (youtu.be/) 처리
        else if (youtubeUrl.includes('youtu.be/')) {
            videoId = youtubeUrl.split('youtu.be/')[1].split(/[?&]/)[0];
        }
        // 일반 URL (v=) 처리
        else if (youtubeUrl.includes('v=')) {
            videoId = youtubeUrl.split('v=')[1].split(/[?&]/)[0];
        }

        if (videoId) {
            return `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
        }

        return '/images/default-thumbnail.png';
    }

    loadCards('popular');
    loadCards('latest');

});
