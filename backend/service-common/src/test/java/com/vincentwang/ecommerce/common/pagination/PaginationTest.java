package com.vincentwang.ecommerce.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PaginationTest {

    @Test
    void calculatesLongAndIntegerOffsets() {
        assertThat(Pagination.offset(3, 25)).isEqualTo(75L);
        assertThat(Pagination.offsetAsInt(3, 25)).isEqualTo(75);
    }

    @Test
    void rejectsInvalidOrOverflowingPagination() {
        assertThatIllegalArgumentException().isThrownBy(() -> Pagination.offset(-1, 10));
        assertThatIllegalArgumentException().isThrownBy(() -> Pagination.offset(0, 0));
        assertThatThrownBy(() -> Pagination.offsetAsInt(Integer.MAX_VALUE, 2)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void calculatesTotalPagesWithoutFloatingPointRounding() {
        assertThat(Pagination.totalPages(0, 20)).isZero();
        assertThat(Pagination.totalPages(20, 20)).isEqualTo(1);
        assertThat(Pagination.totalPages(21, 20)).isEqualTo(2);
    }
}
