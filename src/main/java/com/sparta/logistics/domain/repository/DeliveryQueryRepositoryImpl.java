package com.sparta.logistics.domain.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sparta.logistics.common.constant.UserRole;
import com.sparta.logistics.domain.entity.Delivery;
import com.sparta.logistics.domain.entity.QDelivery;
import com.sparta.logistics.domain.entity.QDeliveryRoute;
import com.sparta.logistics.domain.model.DeliveryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// Spring Data JPA는 커스텀 구현체(Impl)를 "기본 인터페이스와 같은 패키지"에 있을 때만 자동으로 인식한다.
// 그래서 infrastructure 쪽이 아니라 DeliveryQueryRepository와 같은 domain.repository 패키지에 둔다.
@Repository
@RequiredArgsConstructor
public class DeliveryQueryRepositoryImpl implements DeliveryQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Delivery> search(
            DeliveryStatus status,
            UUID hubId,
            UUID currentUserId,
            UserRole role,
            Pageable pageable
    ) {
        // QDelivery, QDeliveryRoute는 직접 만든 게 아니라,
        // 빌드할 때 QueryDSL이 Delivery/DeliveryRoute 엔티티를 보고 자동생성.
        // delivery.status, delivery.departureHubId처럼 필드에 점(.)으로 접근가능하게 해줌
        QDelivery delivery = QDelivery.delivery;
        QDeliveryRoute route = QDeliveryRoute.deliveryRoute;

        BooleanBuilder builder = new BooleanBuilder();

        // soft-delete된 배송은 조회 대상에서 제외
        builder.and(delivery.deletedAt.isNull());

        if (status != null) {
            builder.and(delivery.status.eq(status));
        }

        // hubId는 출발/도착 허브 둘 중 하나만 맞아도 매칭 (해당 허브가 관여된 배송을 찾는다는 의미)
        if (hubId != null) {
            builder.and(
                    delivery.departureHubId.eq(hubId)
                            .or(delivery.destinationHubId.eq(hubId))
            );
        }

        // 역할별 "본인 담당" 제한.
        // MASTER/HUB_MANAGER(담당 허브 확인 불가)/COMPANY_MANAGER(본인 주문건 확인 불가)는 임시 무제한.
        if (role == UserRole.DELIVERY_DRIVER) {
            // "본인이 담당하는 배송" = 전체 배송 담당(companyDriverId)이 나이거나,
            // 소속 구간(route) 중 하나라도 담당(driverId)이 나인 경우 - 원 요구사항(권한 표)의
            // "배송 담당자"가 companyDriverId/route.driverId 두 배정 방식을 구분하지 않아서 OR로 묶는다.
            builder.and(
                    delivery.companyDriverId.eq(currentUserId)
                            .or(
                                    // 상관 서브쿼리(correlated subquery) - EXISTS라서 route를 실제로 안 끌고 온다.
                                    JPAExpressions.selectOne()
                                            .from(route)
                                            .where(
                                                    route.delivery.eq(delivery),
                                                    route.driverId.eq(currentUserId)
                                            )
                                            .exists()
                            )
            );
        }

        // content : 실제 이번 페이지에 보여줄 데이터 목록
        List<Delivery> content = queryFactory
                .selectFrom(delivery)
                .where(builder)
                .orderBy(toOrderSpecifiers(pageable.getSort(), delivery))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // total : 조건에 맞는 전체 개수(페이지 나누기 전 총량)
        Long total = queryFactory
                .select(delivery.count())
                .from(delivery)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /** Pageable의 Sort를 QueryDSL의 OrderSpecifier[]로 변환한다. */
    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort, QDelivery delivery) {
        PathBuilder<Delivery> pathBuilder = new PathBuilder<>(Delivery.class, delivery.getMetadata());

        return sort.stream()
                .map(order -> new OrderSpecifier<>(
                        order.isAscending() ? Order.ASC : Order.DESC,
                        pathBuilder.getComparable(order.getProperty(), Comparable.class)
                ))
                .toArray(OrderSpecifier[]::new);
    }
}
