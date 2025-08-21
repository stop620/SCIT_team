import os
import tempfile
import traceback
import logging
from flask import Flask, request, jsonify
from transcribe_module import Transcriber

# 로깅 설정
logging.basicConfig(level=logging.INFO, format='[%(asctime)s] [%(levelname)s] - %(message)s')

app = Flask(__name__)

# --- 초기화 ---
try:
    # 환경 변수에서 Gemini API 키 목록을 가져옵니다. (예: "key1,key2,key3")
    api_keys_str = os.getenv("GEMINI_API_KEYS")
    if not api_keys_str:
        raise ValueError("환경 변수 'GEMINI_API_KEYS'가 설정되지 않았습니다.")

    api_keys = [key.strip() for key in api_keys_str.split(',')]

    # Transcriber 인스턴스 생성
    transcriber = Transcriber(api_keys)
    logging.info("Flask 서버가 성공적으로 초기화되었습니다.")

except ValueError as e:
    logging.error(f"초기화 실패: {e}")
    transcriber = None # 초기화 실패 시 transcriber를 None으로 설정

@app.route('/api/transcribe', methods=['POST'])
def transcribe_audio():
    """
    YouTube URL과 시간 정보를 받아 자막을 추출하는 API 엔드포인트.
    """
    if transcriber is None:
        return jsonify({"error": "서버가 올바르게 초기화되지 않았습니다. API 키 설정을 확인하세요."}), 500

    logging.info("'/api/transcribe'로 새로운 요청을 받았습니다.")

    data = request.json
    url = data.get('url')
    start_time = data.get('start')
    end_time = data.get('end')

    logging.info(f"요청 데이터: URL={url}, Start={start_time}, End={end_time}")

    if not all([url, start_time is not None, end_time is not None]):
        logging.error("필수 파라미터 누락: url, start, end")
        return jsonify({"error": "url, start, end 파라미터가 필요합니다."}), 400

    # tempfile.TemporaryDirectory()를 사용하여 작업 완료 후 임시 폴더 자동 삭제
    with tempfile.TemporaryDirectory() as temp_dir:
        try:
            logging.info(f"임시 디렉토리 생성: {temp_dir}")

            # 자막 추출 작업 수행
            result = transcriber.process_video_segment(
                temp_dir, url, float(start_time), float(end_time)
            )

            logging.info("작업 성공. 클라이언트에 결과를 반환합니다.")
            return jsonify(result)

        except Exception as e:
            # 실패 시 에러 메시지 반환
            logging.error(f"작업 중 오류 발생: {e}")
            traceback.print_exc() # 콘솔에 전체 에러 스택 출력
            return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    # 로컬에서 직접 실행할 때 사용 (python transcribe_server.py)
    app.run(host='0.0.0.0', port=5001, debug=True)
