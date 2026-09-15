package cl.duoc.digitalfix.bff.dto;

import jakarta.validation.constraints.NotBlank;

public class WorkOrderDto {
    private Long id;

    @NotBlank
    private String descripcion;

    @NotBlank
    private String clienteId;

    private String tecnicoId;
    private String status = "CREADA";

    public WorkOrderDto() {}

    public WorkOrderDto(Long id, String descripcion, String clienteId, String tecnicoId, String status) {
        this.id = id;
        this.descripcion = descripcion;
        this.clienteId = clienteId;
        this.tecnicoId = tecnicoId;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(String tecnicoId) { this.tecnicoId = tecnicoId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
