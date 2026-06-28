package com.example.springjwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringJwtApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringJwtApplication.class, args);
    }

}

// 보안을 위한 JWT 진환
/*
> 토큰 사용 추적
- 기존 JWT의 동작 방식
    - 로그인 성공 JWT 발급: 서버측 -> 클라이언트로 JWT 발급
    - 권한이 필요한 모든 요청: 클라이언트 -> 서버측 JWT 전송

- 권한이 필요한 요청은 서비스에서 많이 발생함
- 따라서 JWT는 매시간 수많은 요청을 위해 클라이언트의 JS 코드로 HTTP 통신을 통해 서버로 전달함
- 해커는 클라이언트 측에서 XSS를 이용하거나 HTTP 통신을 가로채서 토큰을 훔칠 수 있음
- 탈취를 방지하고 탈취되었을 경우 대비 로직이 존재

> 다중 토큰: Refresh 토큰과 생명 주기 (탈취 방지 + 탈취 경우 방지하는 로직도 조금)
- Access/Refresh 토큰의 개념 등장
- 자주 사용되는 토큰의 생명주기는 짧게, 이 토큰이 만료되었을때 함께 받은 Refresh 토큰은 길게해서 이걸 가지고 코튼을 재발급
- 로직
    - 로그인 성공시 생명주기와 활용도가 다른 토큰 2개 발급(Access / Refresh)
        - Access: 권한이 필요한 모든 요청 헤더에 사용될 JWT로 탈취 위험을 낮추기 위해 약 10분 정도의 짧은 주기
        - Refresh: Access 토큰이 만료되었을 경우, 재발급을 위한 용도로만 사용 긴 생명주기를 가짐

    - 권한이 필요한 모든 요청: Access 토큰을 통해 요청
        - Access 토큰만 사용하여 요청을 하여 Refresh 토큰의 호출 및 전송율 빈도가 낮음
    - 권한이 알맞다는 가정하에 2가지 상황: 데이터 응답, 토큰 만료 응답
    - 토큰이 만료된 경우 Refresh 토큰으로 Access 토큰 발급
        - Access 토큰이 만료되었다는 요청이 들어왔을 경우 프론트엔드에서 발급 받은 Refresh 토큰으로 서버의 특정 경로로 요청을 보내어 Access 토큰 재발급
    - 서버측에서는 Refresh 토큰을 검증 후 Access 토큰을 새로 발급

> 다중 토큰 구현 포인트
- 로그인이 완료되면 successHandler에서 Access/Refresh 토큰을 2개를 발급
    - 각기 다른 생명주기, payload 정보를 가짐
- Access 토큰의 요청을 검증하는 JWTFilter에서 Access 토큰이 만료된 경우 프론트 개발자와 협의된 상태 코드와 메시지를 응답
- 프론트엔드에서 만료 응답을 받으면 지정된 경로로 Refresh 토큰을 서버측으로 전송 후 Access 토큰을 재발급 받음
- 서버측에서는 Refresh 토큰을 받을 엔드 포인트를 구성하여 Refresh를 검증하고 Access를 응답함

> Refresh 토큰이 탈취되는 경우
- 단일 -> 다중 토큰으로 전환하며 자주 사용되는 Access 토큰을 탈취되더라도 생명주기가 짧아 피해 확률이 줄어듦
- 하지만 Refresh도 탈취 가능성이 있음
    - Access/Refresh 토큰의 저장 위치 고려
    - Refresh 토큰 Rotate
        - Access 토큰을 갱신하기 위한 Refresh 토큰 요청 시 서버측에서 Refresh 토큰도 재발급을 진행하여 한 번 사용한 Refresh 토큰은 재사용하지 못함

> Access/Refresh 토큰 저장 위치
- 로컬 스토리지 / 쿠키에 대해 고려를 함
    - 로컬 스토리지: XSS 공격에 취약함: Access 토큰 저장
    - httpOnly 쿠기: CSRF 공격에 취약함: Refresh 토큰 저장

> 로그아웃과 Refresh 토큰 주도권
- 문제
    - 로그아웃을 구현해도 JWT를 이미 탈취 당했다면 서버는 주도권이 없음

- 방어 방법
    - 생명주기가 긴 Refresh 토큰을 발급과 함께 서버측에 같이 저장
    - 로그아웃을 하면, 서버측의 저장소에서도 함께 삭제

> 로그인 시 메일 알림
- 사용하지 않던 IP나 브라우저에서 접근할 경우, 사용자 계정으로 메일 알림 전송
- 아니요를 클릭하면 서버측 토큰 저장소에서 해당 유저에 대한 Refresh 토큰을 모두 제거하여 앞으로의 인증을 막을 수 있음
*/

// Refresh Rotate
/*
> 장단점
- 장점
    - 보안성 강화
    - 로그인 지속 시간이 길어짐
- 추가 구현 작업
    - 발급했던 Refresh 토큰을 모두 기억한 뒤, Rotate 이전의 Refresh 토큰은 사용하지 못하게 해야함

> 주의점
    - Rotate 되기 이전의 토큰을 가지고 서버측으로 가도 인증이 되기 때문에 서버측에서 발급했던 Refresh들을 기억한 뒤 블랙리스트 처리를 진행하는 로직을 작성해야함
*/

// Refresh 토큰 서버측 저장
/*
> 구현 방법
- 발급시
    - Refresh 토큰을 서버측 저장소에 저장
- 갱신시
    - 기존 Refresh 토큰을 삭제하고 새로 발급한 Refresh 토큰을 저장

> 토큰 저장소 구현
- 토큰 저장소
    - RDB 또는 Redis를 주로 사용(Redis의 경우 특정 시간이 지나면 자동 삭제할 수 있는 장점이 있음)
- RefreshEntity

> Refresh 토큰 저장소에서 기한이 지난 토큰 삭제
- TTL 설정으로 통해 자동으로 Refresh 토큰이 삭제되면 무방하지만 계속해서 토큰이 쌓일 경우 용량 문제 발생
- 스케쥴 작업을 통해 만료 시간이 지난 토큰은 주기적으로 삭제하는 것이 좋음
*/

// 로그아웃
/*
> 로그아웃 기능을 통해 추가적인 JWT 탈취 시간을 줄일 수 있음
- 버튼 클릭시
    - 로컬 스토리지에 존재하는 Access 토큰 삭제 및 서버측 로그아웃 경로로 요청을 보냄
    - 백엔드측: 로그아웃 로직을 추가 구현하여 Refresh 토큰을 받아 쿠키 초기화 후 Refresh DB에서 해당 Refresh 토큰 삭제
        - 모든 계정에서 로그아웃 구현시, username 기반으로 모든 Refresh 토큰 삭제
*/

// 추가적인 보안 구상
/*
> 요청 IP 확인: PC 기반
- PC의 경우 IP 주소가 변경될 일이 거의 없음
- IP 주소가 변경되는 경우 요청이 거부되도록 진행할 수 있음
- 로직 구상
    - 로그인 시 JWT 발급과 함께 JWT와 IP를 DB 테이블에 저장
    - Access 토큰으로 요청 시 요청 IP와 로그인 시 지정한 IP 주소를 대조
    - Access 토큰 재발급 시 새로운 Access 토큰과 IP를 DB 테이블에 저장

- 네이버의 경우
    - 네이버도 PC 환경에서 로그인을 진행한 후 다른 IP 줒소로 변경되면 재로그인을 진행하라는 알림이 발생함
*/
