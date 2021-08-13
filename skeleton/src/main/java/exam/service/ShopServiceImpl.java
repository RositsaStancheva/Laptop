package exam.service;

import exam.model.entity.Shop;
import exam.model.entity.dto.ShopSeedRootDto;
import exam.repository.ShopRepository;
import exam.util.ValidationUtil;
import exam.util.XmlParser;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ShopServiceImpl implements ShopService {
    public static final String SHOPS_FILE_PATH = "src/main/resources/files/xml/shops.xml";

    private final ShopRepository shopRepository;
    private final XmlParser xmlParser;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;
    private final TownService townService;

    public ShopServiceImpl(ShopRepository shopRepository, XmlParser xmlParser, ValidationUtil validationUtil, ModelMapper modelMapper, TownService townService) {
        this.shopRepository = shopRepository;
        this.xmlParser = xmlParser;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
        this.townService = townService;
    }

    @Override
    public boolean areImported() {
        return shopRepository.count() > 0;
    }

    @Override
    public String readShopsFileContent() throws IOException {
        return Files.readString(Path.of(SHOPS_FILE_PATH));
    }

    @Override
    public String importShops() throws JAXBException, FileNotFoundException {
        StringBuilder sb = new StringBuilder();

        xmlParser.fromFile(SHOPS_FILE_PATH, ShopSeedRootDto.class)
                .getShops()
                .stream().distinct().filter(shopSeedDto -> {
                    boolean isValid = validationUtil.isValid(shopSeedDto);
                    if (shopRepository.existsByName(shopSeedDto.getName())) {
                        sb.append("Invalid shop").append(System.lineSeparator());
                    } else if (isValid) {
                        sb.append(String.format("Successfully imported Shop %s - %f",
                                        shopSeedDto.getName(), shopSeedDto.getIncome()))
                                .append(System.lineSeparator());
                    } else {
                        sb.append("Invalid shop").append(System.lineSeparator());
                    }

                    return isValid;
                })
                .map(shopSeedDto -> {
                    Shop shop = modelMapper.map(shopSeedDto, Shop.class);
                    shop.setTown(townService.findByName(shopSeedDto.getTown().getName()));
                    return shop;
                })
                .forEach(shopRepository::save);


        return sb.toString();
    }

    @Override
    public Shop findShopByName(String name) {
        return shopRepository.findByName(name);
    }
}
