package cl.duoc.digitalfix.bff.repository;

import cl.duoc.digitalfix.bff.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findByStatusIgnoreCase(String status);
}
