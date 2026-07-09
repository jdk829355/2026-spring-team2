package team2.goodsmap.planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team2.goodsmap.planner.entity.Planner;

import java.time.LocalDate;
import java.util.Optional;

public interface PlannerRepository extends JpaRepository<Planner, Long> {

    // userId + date로 플래너 조회 (내가 살 것 담기 - 날짜 기준 플래너 찾기/생성 판단용)
    // user.id를 타고 들어가는 조건이라 "User_Id"처럼 언더스코어로 구분해야 함
    Optional<Planner> findByUser_IdAndDate(Long userId, LocalDate date);
}
