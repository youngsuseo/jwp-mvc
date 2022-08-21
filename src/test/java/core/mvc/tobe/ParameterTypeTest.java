package core.mvc.tobe;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterTypeTest {

    @Test
    void casting() {
        Object casting = ParameterTypeEnum.casting("1", int.class);
//        Long casting1 = ParameterType.casting("2", Long.class);
    }

//    @Test
//    void primitiveType() {
//        assertThat(ParameterTypeEnum.primitiveType(int.class)).isTrue();
//        assertThat(ParameterTypeEnum.primitiveType(long.class)).isTrue();
//        assertThat(ParameterTypeEnum.primitiveType(String.class)).isFalse();
//    }
}