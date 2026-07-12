package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.OrderTodoEntity;
import com.daccaauto.pos.entity.OrderTodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderTodoRepository extends JpaRepository<OrderTodoEntity, Long> {

    List<OrderTodoEntity> findByStatusInOrderByUpdatedAtDesc(Collection<OrderTodoStatus> statuses);

    @Query(
        value = """
            select todo
            from OrderTodoEntity todo
            join todo.product product
            where todo.status in :statuses
              and (:keywordPattern is null
                   or lower(product.name) like :keywordPattern
                   or lower(product.partNumber) like :keywordPattern
                   or lower(product.category.name) like :keywordPattern
                   or lower(product.brand.name) like :keywordPattern
                   or lower(coalesce(todo.note, '')) like :keywordPattern)
            order by todo.updatedAt desc
            """,
        countQuery = """
            select count(todo)
            from OrderTodoEntity todo
            join todo.product product
            where todo.status in :statuses
              and (:keywordPattern is null
                   or lower(product.name) like :keywordPattern
                   or lower(product.partNumber) like :keywordPattern
                   or lower(product.category.name) like :keywordPattern
                   or lower(product.brand.name) like :keywordPattern
                   or lower(coalesce(todo.note, '')) like :keywordPattern)
            """
    )
    Page<OrderTodoEntity> searchOpenTodos(Collection<OrderTodoStatus> statuses, String keywordPattern, Pageable pageable);

    Optional<OrderTodoEntity> findFirstByProductIdAndStatusIn(Long productId, Collection<OrderTodoStatus> statuses);

    List<OrderTodoEntity> findByProductIdAndStatusIn(Long productId, Collection<OrderTodoStatus> statuses);
}
