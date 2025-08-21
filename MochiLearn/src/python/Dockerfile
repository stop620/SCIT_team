# src/python/Dockerfile

# 1. 베이스 이미지로 Python 3.10 버전을 사용
FROM python:3.10-slim

# 2. 시스템 패키지 업데이트 및 ffmpeg 설치
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*

# 3. 작업 디렉토리 설정
WORKDIR /app

# 4. requirements.txt 파일을 먼저 복사하여 라이브러리 설치 (캐시 활용)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 5. 나머지 소스 코드 전체를 복사
COPY . .

# 6. 컨테이너 실행 시 실행할 명령어 수정
#    "app:app" -> "transcribe_server:app"
CMD ["gunicorn", "--bind", "0.0.0.0:5001", "transcribe_server:app"]