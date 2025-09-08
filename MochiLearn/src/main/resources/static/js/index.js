document.addEventListener('DOMContentLoaded', () => {

    const section = document.querySelector(".hero-section");
    const container = section.querySelector(".carousel-container");
    const inner = section.querySelector(".carousel-inner");
    const slides = section.querySelectorAll(".carousel-slide");
    const slideBtn = section.querySelectorAll(".nav-btn");
    const slideDot = section.querySelector(".carousel-indicators");

    let currentSlide = 0;               // 현재 컨텐츠
    const totalSlides = slides.length;  // 전체 컨텐츠 수
    let autoSlideInterval;        // 컨텐츠 변경 간격
    let dotIndex = "";
    let sliderWidth = slides[0].clientWidth;


    // 인디케이터 업데이트
    function updateIndicators() {
        const indicators = document.querySelectorAll('.indicator-btn');
        indicators.forEach((btn, index) => {
            if (index === currentSlide) {
                btn.classList.add('active');
                // console.log(index);

            } else {
                btn.classList.remove('active');
            }
        });
    }
    // 좌우 한 슬라이드 이동
    function moveSlide(direction) {
        currentSlide = (currentSlide + direction + totalSlides) % totalSlides;
        const offset = -sliderWidth * currentSlide;
        inner.style.transform = `translateX(${offset}px)`;
        updateIndicators();
    }

    // 슬라이드 인덱스로 이동
    function goToSlide(index) {
        currentSlide = index;
        const offset = -sliderWidth * index;
        inner.style.transform = `translateX(${offset}px)`;
        updateIndicators();
    }

    // 자동 슬라이드 시작
    function startAutoSlide() {
        autoSlideInterval = setInterval(() => {
            moveSlide(1);
        }, 5000); // 5초마다 자동 슬라이드
    }
    // 자동 슬라이드 멈춤
    function stopAutoSlide() {
        clearInterval(autoSlideInterval);
    }


    // 페이지 로드 시 인디케이터 생성, 초기화
    for (let i = 0; i < totalSlides; i++) {
        const btn = document.createElement('button');
        btn.className = 'indicator-btn';
        btn.onclick = () => goToSlide(i);
        slideDot.appendChild(btn);
    }
    inner.style.transition = "all 0.6s";
    updateIndicators();
    startAutoSlide();

    section.addEventListener('mouseenter', stopAutoSlide);
    section.addEventListener('mouseleave', startAutoSlide);

    slideBtn.forEach((btn, index) => {
        btn.addEventListener("click", () => {
            if(btn.classList.contains("prev-btn")) {
                moveSlide(-1);
            } else {
                moveSlide(1);
            }
        });
    });

});