document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM 로드')
    let books = [];

    const wordContainer = document.querySelector('.wordContainer');

    // 단어장 목록 로딩
    function loadBooks() {
        console.log('단어장 요청');
        fetch(`/mochilearn/api/wordbook/list`)
            .then(response => response.json())
            .then(data => {
                console.log('단어장 받음');

                books = data.data;
                console.log(books);

                renderBooks();
            })
    }


    // 단어장 렌더링
    function renderBooks() {

        let bookHtml = '';

        books.forEach((book,index) => {
            bookHtml += `
                <div class="wordList">
                <div class="wordCard">
                    <a href="/mochilearn/page/wordCard?bookId=${book.id}">${book.title}</a>
                </div>
            </div>
            `;
        });

        wordContainer.innerHTML = bookHtml +
            `<div class="addWordList">
                <div class="wordCard">
                    <a href="/mochilearn/page/addWordCard">단어장 추가</a>
                </div>
            </div>`;

    }

    // 시작
    loadBooks();
});