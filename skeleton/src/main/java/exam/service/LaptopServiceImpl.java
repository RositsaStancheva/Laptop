package exam.service;

import com.google.gson.Gson;
import exam.model.entity.Laptop;
import exam.model.entity.dto.LaptopSeedDto;
import exam.repository.LaptopRepository;
import exam.util.ValidationUtil;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Service
public class LaptopServiceImpl implements LaptopService {
    private static final String LAPTOP_FILE_PATH = "src/main/resources/files/json/laptops.json";

    private final LaptopRepository laptopRepository;
    private final Gson gson;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;
    private final ShopService shopService;

    public LaptopServiceImpl(LaptopRepository laptopRepository, Gson gson, ValidationUtil validationUtil, ModelMapper modelMapper, ShopService shopService) {
        this.laptopRepository = laptopRepository;
        this.gson = gson;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
        this.shopService = shopService;
    }

    @Override
    public boolean areImported() {
        return laptopRepository.count() > 0;
    }

    @Override
    public String readLaptopsFileContent() throws IOException {
        return Files.readString(Path.of(LAPTOP_FILE_PATH));
    }

    @Override
    public String importLaptops() throws IOException {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(gson.fromJson(readLaptopsFileContent(), LaptopSeedDto[].class))
                .filter(laptopSeedDto -> {
                    boolean isValid = validationUtil.isValid(laptopSeedDto);
                    if(isValid && !laptopRepository.existsByMacAddress(laptopSeedDto.getMacAddress())){
                        sb.append(String.format("Successfully imported Laptop %s - %.2f - %d - %d",
                                laptopSeedDto.getMacAddress(),
                                laptopSeedDto.getCpuSpeed(),
                                laptopSeedDto.getRam(),
                                laptopSeedDto.getStorage())).append(System.lineSeparator());
                    }else{
                        sb.append("Invalid Laptop").append(System.lineSeparator());
                    }
                    return isValid;
                })
                .map(laptopSeedDto -> {
                    Laptop laptop = modelMapper.map(laptopSeedDto, Laptop.class);
                    laptop.setShop(shopService.findShopByName(laptopSeedDto.getShop().getName()));
                    return laptop;
                })
                .forEach(laptopRepository::save);





        return sb.toString();
    }

    @Override
    public String exportBestLaptops() {
        return null;
    }
}
