package host_verto_empb_com_pl_server;

import pl.com.stream.lib.commons.datasource.dataset.validation.IgnoredValidationWarnings
import pl.com.stream.next.asen.server.info.RequestContextManager
import pl.com.stream.verto.cmm.employee.server.pub.main.EmployeeDto
import pl.com.stream.verto.cmm.employee.server.pub.main.EmployeeService
import pl.com.stream.verto.cmm.labourresource.server.pub.employee.LabourResourceEmployeeDto
import pl.com.stream.verto.cmm.labourresource.server.pub.employee.LabourResourceEmployeeService
import pl.com.stream.verto.cmm.labourresource.server.pub.main.LabourResourceDto
import pl.com.stream.verto.cmm.labourresource.server.pub.main.LabourResourceService
import pl.com.stream.verto.cmm.operator.server.pub.main.OperatorDto
import pl.com.stream.verto.cmm.operator.server.pub.main.OperatorService

class Generowanie_zasobu_na_podstawie_nazwy_operatora extends pl.com.stream.next.asen.common.groovy.ServerScriptEnv {
    def script() {

        final Long idLabourResourceType = 100000L;
        final Long idScheduleCalendar = 100000L;

        Long idOperator = inParams.idOperator;

        if (idOperator.equals(null)) {
            requestContext.getReport().println("Brak operatora. Przed uruchomieniem skryptu zapisz aktualne dane");
            return;
        }

        OperatorService operatorService = context.getService(OperatorService.class);
        RequestContextManager requestContextManager = context.getService(RequestContextManager.class);

        requestContextManager.setIgnoredValidationWarnings(IgnoredValidationWarnings.IGNORE_ALL_VALIDATION_WARNING);


        OperatorDto operatorDto = operatorService.find(idOperator);


        //Utworzenie pracownika

        String sql="""
                    Select EM.CODE from verto.EMPLOYEE_V EM where EM.CODE =:CODE
                   """
        def listEmployye = dm.executeSQLQuery(sql, ["CODE" : idOperator.toString()]);



        if (listEmployye.size() > 0 ) {
            requestContext.getReport().println("Pracownik dla edytowanego operatora został już dodany");
            return;
        }

        EmployeeService employeeService = context.getService(EmployeeService.class);
        EmployeeDto employeeDto = new EmployeeDto();

        employeeDto.firstname = operatorDto.firstName;
        employeeDto.surname = operatorDto.lastName;
        employeeDto.idOperator = operatorDto.idOperator;
        employeeDto.pesel = operatorDto.idOperator;

        employeeDto = employeeService.init(employeeDto);
        employeeDto.code =  operatorDto.idOperator;

        Long idEmployee = employeeService.insert(employeeDto);

        //Dodanie zasobu (jednostka robocza)

        LabourResourceService labourResourceService = context.getService(LabourResourceService.class);

        LabourResourceDto labourResourceDto = new LabourResourceDto();

        labourResourceDto.name = operatorDto.firstName + ' ' + operatorDto.lastName;
        labourResourceDto.idLabourResourceType = idLabourResourceType;
        labourResourceDto.idScheduleCalendar = idScheduleCalendar;

        labourResourceDto = labourResourceService.init(labourResourceDto);

        Long idLabourResource = labourResourceService.insert(labourResourceDto);

        //Powiazanie pracownika z zasobem

        LabourResourceEmployeeService labourResourceEmployeeService = context.getService(LabourResourceEmployeeService.class);

        LabourResourceEmployeeDto labourResourceEmployeeDto = new LabourResourceEmployeeDto();

        labourResourceEmployeeDto.idEmployee = idEmployee;
        labourResourceEmployeeDto.idLabourResource = idLabourResource;

        labourResourceEmployeeDto = labourResourceEmployeeService.init(labourResourceEmployeeDto);

        labourResourceEmployeeService.insert(labourResourceEmployeeDto);

        requestContext.getReport().println("Utworzono zasób o nazie: " + operatorDto.firstName + ' ' + operatorDto.lastName);
    }
}