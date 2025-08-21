import os
import subprocess
import json
import google.generativeai as genai
import logging

# 로깅 설정
logging.basicConfig(level=logging.INFO, format='[%(asctime)s] [%(levelname)s] - %(message)s')

class Transcriber:
    """
    오디오 추출 및 Gemini API 호출을 담당하는 클래스.
    """
    def __init__(self, api_keys: list):
        if not api_keys or not all(api_keys):
            raise ValueError("API 키 리스트가 비어있거나 유효하지 않습니다.")
        self.api_keys = api_keys
        self.current_key_index = 0
        logging.info(f"Transcriber가 {len(self.api_keys)}개의 API 키로 초기화되었습니다.")

    def _call_gemini_api(self, audio_file_path: str) -> str:
        """
        오디오 파일을 Gemini API로 보내 자막을 추출합니다.
        API 키를 순환하며 사용하고, 실패 시 다음 키로 재시도합니다.
        """
        prompt = """Transcribe this audio file into Japanese and Korean.
                     If multiple lines appear in the file, short lines such as oh and ah may be omitted.
                     All answers, whether one or more, are in JSON format
                     {index: order, time: time from start time (seconds to 3 decimal places only), japanese: Japanese, korean: Korean}."""

        # 모든 키를 한 번씩 시도해볼 수 있도록 루프를 설정합니다.
        for i in range(len(self.api_keys)):
            # 현재 인덱스에 해당하는 키를 가져옵니다.
            api_key = self.api_keys[self.current_key_index]

            try:
                logging.info(f"Gemini API 호출 시도 ({i+1}/{len(self.api_keys)})... Key: ...{api_key[-4:]}")

                genai.configure(api_key=api_key)
                model = genai.GenerativeModel('models/gemini-1.5-flash')

                logging.info(f"오디오 파일 업로드 중: {audio_file_path}")
                audio_file = genai.upload_file(path=audio_file_path)

                logging.info("Gemini에게 콘텐츠 생성 요청 중...")
                response = model.generate_content([prompt, audio_file])

                # 다음 요청이나 다음 재시도를 위해 키 인덱스를 미리 업데이트합니다.
                self.current_key_index = (self.current_key_index + 1) % len(self.api_keys)

                if response.text:
                    logging.info("Gemini로부터 유효한 응답을 받았습니다.")
                    return response.text # 성공 시 즉시 반환
                else:
                    logging.warning("Gemini로부터 빈 응답을 받았습니다. 다음 키로 재시도합니다.")

            except Exception as e:
                logging.error(f"API 호출 중 오류 발생: {e}. 다음 키로 재시도합니다.")
                # 오류 발생 시에도 다음 요청을 위해 키 인덱스를 업데이트합니다.
                self.current_key_index = (self.current_key_index + 1) % len(self.api_keys)

        raise Exception("모든 API 키를 사용했지만 Gemini API 호출에 실패했습니다.")

    def _parse_gemini_response(self, text: str) -> list:
        """Gemini 응답에서 Markdown 코드 블록을 제거하고 JSON으로 파싱합니다."""
        logging.info("Gemini 응답 파싱 시작...")

        cleaned_text = text.strip()
        if cleaned_text.startswith("```json"):
            cleaned_text = cleaned_text[7:-3].strip()
        elif cleaned_text.startswith("```"):
            cleaned_text = cleaned_text[3:-3].strip()

        logging.debug(f"파싱할 JSON 문자열: {cleaned_text}")

        try:
            parsed_json = json.loads(cleaned_text)
            logging.info("Gemini 응답 파싱 완료.")
            return parsed_json
        except json.JSONDecodeError as e:
            logging.error(f"JSON 파싱 실패. Gemini가 유효하지 않은 응답을 반환했습니다. 응답 내용: {cleaned_text}")
            # 원래 오류에 추가적인 컨텍스트를 더해서 다시 예외를 발생시킴
            raise ValueError(f"Gemini response is not a valid JSON: {cleaned_text}") from e


    def process_video_segment(self, temp_dir: str, url: str, start_time: float, end_time: float) -> list:
        """
        가장 안정적인 후처리 방식으로 오디오를 다운로드하고 잘라낸 후,
        Gemini API를 호출하여 결과를 반환합니다.
        """
        output_path = os.path.join(temp_dir, "sliced_audio.mp3")
        duration = end_time - start_time

        # FFmpeg 후처리 인자 설정
        ffmpeg_args = f"-ss {start_time:.3f} -t {duration:.3f}"

        # yt-dlp 명령어
        command = [
            "yt-dlp",
            "-f", "bestaudio",
            "-x",  # --extract-audio
            "--audio-format", "mp3",
            "--postprocessor-args", ffmpeg_args,
            "-o", output_path,
            url,
        ]

        logging.info(f"yt-dlp 실행: {' '.join(command)}")

        # subprocess.run을 사용하여 외부 명령어 실행
        result = subprocess.run(command, capture_output=True, text=True, check=False, encoding='utf-8')

        # yt-dlp의 출력을 모두 로깅
        if result.stdout:
            logging.info(f"yt-dlp STDOUT:\n{result.stdout}")
        if result.stderr:
            logging.error(f"yt-dlp STDERR:\n{result.stderr}")

        if result.returncode != 0:
            raise Exception(f"yt-dlp 실행 실패 (종료 코드: {result.returncode})")

        logging.info(f"오디오 파일 생성 완료: {output_path}")

        # Gemini API 호출 및 결과 파싱
        gemini_response_text = self._call_gemini_api(output_path)
        parsed_result = self._parse_gemini_response(gemini_response_text)

        return parsed_result
