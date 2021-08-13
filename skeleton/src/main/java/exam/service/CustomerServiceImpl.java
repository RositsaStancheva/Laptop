package exam.service;

import com.google.gson.Gson;
import exam.model.entity.Customer;
import exam.model.entity.dto.CustomerSeedDto;
import exam.repository.CustomerRepository;
import exam.util.ValidationUtil;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Service
public class CustomerServiceImpl implements CustomerService {

    public static final String CUSTOMER_FILE_PATH = "src/main/resources/files/json/customers.json";

    private final CustomerRepository customerRepository;
    private final ModelMapper modelMapper;
    private final Gson gson;
    private final ValidationUtil validationUtil;
    private final TownService townService;

    public CustomerServiceImpl(CustomerRepository customerRepository, ModelMapper modelMapper, Gson gson, ValidationUtil validationUtil, TownService townService) {
        this.customerRepository = customerRepository;
        this.modelMapper = modelMapper;
        this.gson = gson;
        this.validationUtil = validationUtil;
        this.townService = townService;
    }

    @Override
    public boolean areImported() {
        return customerRepository.count() > 0;
    }

    @Override
    public String readCustomersFileContent() throws IOException {
        return Files.readString(Path.of(CUSTOMER_FILE_PATH));
    }

    @Override
    public String importCustomers() throws IOException {
        StringBuilder sb = new StringBuilder();

        Arrays.stream(gson.fromJson(readCustomersFileContent(), CustomerSeedDto[].class))
                .filter(customerSeedDto -> {
                    boolean isValid = validationUtil.isValid(customerSeedDto);

                    if(isValid && !customerRepository.existsByEmail(customerSeedDto.getEmail())){
                        sb.append(String.format("Successfully imported Customer %s %s - %s",
                                customerSeedDto.getFirstName(),customerSeedDto.getLastName(),
                                customerSeedDto.getEmail())).append(System.lineSeparator());
                    }else{
                        sb.append("Invalid Customer").append(System.lineSeparator());
                    }
                    return isValid;
                })
                .map(customerSeedDto -> {
                    Customer customer = modelMapper.map(customerSeedDto, Customer.class);
                    customer.setTown(townService.findByName(customerSeedDto.getTown().getName()));

                    return customer;
                })
                .forEach(customerRepository::save);


        return sb.toString();
    }
}
