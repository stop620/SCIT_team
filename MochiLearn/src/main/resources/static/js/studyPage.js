$(document).ready(function() {
    let page = 0;
    const size = 12;
    let sort = 'popular';
    let loading = false;
    let endReached = false;

    let isTagFilterActive = false;
    let tagFilterPage = 0;
    let tagFilterEnd = false;
    let currentTagFilterTags = [];

    // 모달 열기
    $('#tagBtn').click(function() {
        $('#tagModal').fadeIn(200).css('display', 'flex');
    });

    // 모달 닫기 (X 버튼 및 바깥 영역 클릭)
    $('.close-btn').click(() => $('#tagModal').fadeOut(200));
    $(window).click(function(e) {
        if ($(e.target).is('#tagModal')) {
            $('#tagModal').fadeOut(200);
        }
    });

    // 태그 선택 토글
    $('#tagList').on('click', '.tagSelect', function() {
        $(this).toggleClass('selected');
    });

    // 필터 적용 버튼 클릭
    $('#applyTagFilter').click(function() {
        const selectedTags = $('.tagSelect.selected').map(function() {
            return $(this).data('value');
        }).get();

        

        currentTagFilterTags = selectedTags;
        isTagFilterActive = true;
        tagFilterPage = 0;
        tagFilterEnd = false;

        loadCardsByTags(true);
        $('#tagModal').fadeOut(200);
    });

    // 난이도와 태그 포함 카드 렌더링 함수
    function renderCards(cards, reset = false) {
        if (reset) $('#cardGrid').empty();

        cards.forEach(card => {
            const thumbUrl = getYoutubeThumbnail(card.url);

            let difficultyClass = '';
            let levelKor = '';
            switch(card.level) {
                case '1': difficultyClass = 'beginner'; levelKor = '초급'; break;
                case '2': difficultyClass = 'intermediate'; levelKor = '중급'; break;
                case '3': difficultyClass = 'advanced'; levelKor = '고급'; break;
            }

            let tags = card.tag ? card.tag.split(',').map(t => t.trim()) : [];
            let tagHtml = tags.map(tag => `<span class="genre-tag-item">${tag}</span>`).join('');

            const cardHtml = `
                <div class="card" onclick="location.href='/mochilearn/page/studyCard?cardId=${card.id}'">
					<div class="video-thumbnail">
		                    <img src="${thumbUrl}" alt="썸네일 이미지" />
							<div class="play-icon">▶</div>
					</div>
					<div class="video-info">
                    <h3 class="video-title">${card.title}</h3>
					
                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <span class="difficulty-tag ${difficultyClass}">${levelKor}</span>
                        <div style="color: #FFBB46; font-weight: 600;">⭐ ${card.like || 0}</div>
                    </div>
                    <div class="tag-container" style="display:flex; gap:0.5rem; flex-wrap:wrap;">
                        ${tagHtml}
                    </div>
                </div>
			</div>`;

            $('#cardGrid').append(cardHtml);
        });
    }

    // 유튜브 썸네일 추출 다양한 URL 대응
    function getYoutubeThumbnail(url) {
        if (!url) return '/images/default-thumbnail.png';

        let videoId = '';
        if (url.includes('youtube.com/shorts/')) videoId = url.split('youtube.com/shorts/')[1].split(/[?&]/)[0];
        else if (url.includes('youtu.be/')) videoId = url.split('youtu.be/')[1].split(/[?&]/)[0];
        else if (url.includes('v=')) videoId = url.split('v=')[1].split(/[?&]/)[0];

        return videoId ? `https://img.youtube.com/vi/${videoId}/hqdefault.jpg` : '/images/default-thumbnail.png';
    }

    // 일반 카드 로드 (검색, 정렬 반영)
    function loadCards(reset = false) {
        if (loading || endReached || isTagFilterActive) return;

        loading = true;
        toggleLoading(1);

        if (reset) {
            page = 0;
            endReached = false;
        }

        $.get('/mochilearn/api/study/load', {
            sort,
            page,
            size,
            search: $('#searchInput').val().trim()
        })
            .done(data => {
                if ((!data || data.length === 0) && $('#searchInput').val().trim() !== '') {
                    alert('검색 결과가 없습니다.');
                    $('#searchInput').val('');
                    endReached = true;
                    loading = false;
                    return;
                }
                renderCards(data, reset);
                page++;
            })
            .fail(() => {
                alert('카드 로드 중 오류가 발생했습니다.');
            })
            .always(() => {
                loading = false;
                toggleLoading(0);
            });
    }

    // 태그 필터 카드 로드 함수
    function loadCardsByTags(reset = false) {
        if (loading || tagFilterEnd) return;

        loading = true;
        toggleLoading(1);

        if (reset) {
            tagFilterPage = 0;
            tagFilterEnd = false;
            $('#cardGrid').empty();
        }

        $.ajax({
            url: `/mochilearn/api/study/filterByTags?page=${tagFilterPage}&size=${size}&sort=${sort}`,
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(currentTagFilterTags),
            success: function(response) {
                if (!response || response.content.length === 0) {
                    if (reset) alert('해당 태그를 가진 카드가 없습니다.');
                    tagFilterEnd = true;
                    loading = false;
                    return;
                }
                renderCards(response.content, reset);
                tagFilterPage++;
            },
            error: function() {
                alert('태그 필터링 중 오류가 발생했습니다.');
            },
            complete: function() {
                loading = false;
                toggleLoading(0);
            }
        });
    }

    // 정렬 변경 시 처리
    $('#sortBox').change(function() {
        sort = $(this).val();
        if (isTagFilterActive) {
            tagFilterPage = 0;
            tagFilterEnd = false;
            loadCardsByTags(true);
        } else {
            page = 0;
            endReached = false;
            loadCards(true);
        }
    });

    // 검색 버튼 클릭 및 엔터키 처리
    $('#searchBtn').on('click', () => {
        isTagFilterActive = false;
        page = 0;
        endReached = false;
        loadCards(true);
    });

    $('#searchInput').on('keydown', (e) => {
        if (e.key === 'Enter') {
            isTagFilterActive = false;
            page = 0;
            endReached = false;
            loadCards(true);
        }
    });

    // 무한 스크롤 페이징 처리
    $(window).scroll(() => {
        if ($(window).scrollTop() + $(window).height() > $(document).height() - 100) {
            if (loading) return;

            if (isTagFilterActive) {
                if (!tagFilterEnd) loadCardsByTags();
            } else {
                if (!endReached) loadCards();
            }
        }
    });

    // 초기 데이터 로드
    loadCards(true);

    // 위로가기 버튼 클릭
    $('#scrollTopBtn').on('click', () => {
        $('html, body').animate({ scrollTop: 0 }, 300);
    });
});

function toggleLoading(flag) {
    if(flag == 0) {
        $('.loading-spinner').hide();
        $('#cardGrid').css("padding", "2rem 15px 10rem");
    } else {
        $('.loading-spinner').show();
        $('#cardGrid').css("padding", "2rem 15px");
    }
}