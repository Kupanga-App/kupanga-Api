package com.kupanga.api.stockage.service;

import com.kupanga.api.immobilier.mapper.BienMapper;
import com.kupanga.api.immobilier.mapper.ContratMapper;
import com.kupanga.api.immobilier.mapper.PhotoBienUrlMapper;
import com.kupanga.api.user.mapper.PhotoProfilUrlMapper;
import com.kupanga.api.user.mapper.UserMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * S2 : mappers MapStruct générés, avec leurs convertisseurs de photos (clé → URL) branchés sur
 * {@link StockageFactice}, comme Spring le ferait.
 */
public final class MappersDeTest {

    private MappersDeTest() {
    }

    public static UserMapper userMapper() {
        UserMapper mapper = Mappers.getMapper(UserMapper.class);
        ReflectionTestUtils.setField(mapper, "photoProfilUrlMapper", photoProfil());
        return mapper;
    }

    public static BienMapper bienMapper() {
        BienMapper mapper = Mappers.getMapper(BienMapper.class);
        ReflectionTestUtils.setField(mapper, "photoProfilUrlMapper", photoProfil());
        ReflectionTestUtils.setField(mapper, "photoBienUrlMapper", new PhotoBienUrlMapper(new StockageFactice()));
        return mapper;
    }

    public static ContratMapper contratMapper() {
        ContratMapper mapper = Mappers.getMapper(ContratMapper.class);
        ReflectionTestUtils.setField(mapper, "photoProfilUrlMapper", photoProfil());
        return mapper;
    }

    private static PhotoProfilUrlMapper photoProfil() {
        return new PhotoProfilUrlMapper(new StockageFactice());
    }
}
