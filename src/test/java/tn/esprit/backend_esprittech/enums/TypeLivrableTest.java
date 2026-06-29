package tn.esprit.backend_esprittech.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeLivrableTest {

    @Test
    void values_shouldExposeLegacyLivrableTypesInStableOrder() {
        assertArrayEquals(
                new TypeLivrable[]{
                        TypeLivrable.DOCUMENTATION,
                        TypeLivrable.CODE_SOURCE,
                        TypeLivrable.RAPPORT,
                        TypeLivrable.PRESENTATION,
                        TypeLivrable.AUTRE
                },
                TypeLivrable.values()
        );
        assertEquals(TypeLivrable.CODE_SOURCE, TypeLivrable.valueOf("CODE_SOURCE"));
    }
}
