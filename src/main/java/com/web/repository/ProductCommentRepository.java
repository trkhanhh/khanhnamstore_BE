package com.web.repository;

import com.web.dto.response.ProductCommentResponse;
import com.web.entity.Category;
import com.web.entity.ProductComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductCommentRepository extends JpaRepository<ProductComment,Long> {

    @Query("select p from ProductComment p where p.product.id = ?1")
    public List<ProductComment> findByProductId(Long productId);

    @Query("select cm from ProductComment cm order by cm.id desc")
    List<ProductComment> getMostOfComments();
}
