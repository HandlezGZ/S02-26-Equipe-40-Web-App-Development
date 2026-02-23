package
com.nocountry.conversionflow.conversionflow_api.domain.repository;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Conversion;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.ConversionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversionRepository extends JpaRepository<Conversion, Long> {

    /**
     * Buscar conversão por lead
     */
    Optional<Conversion> findByLead(Lead lead);

    /**
     * Buscar conversão por lead e status
     */
    Optional<Conversion> findByLeadAndStatus(Lead lead, ConversionStatus status);
}
