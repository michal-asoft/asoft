package host_verto_empb_com_pl_server;

import pl.com.stream.lib.commons.date.Date
import pl.com.stream.lib.commons.math.Decimal
import pl.com.stream.lib.commons.report.Report
import pl.com.stream.lib.commons.report.ReportLine
import pl.com.stream.verto.cmm.attribute.server.pub.value.AttributeValueService
import pl.com.stream.verto.cmm.operator.server.pub.main.OperatorDto
import pl.com.stream.verto.cmm.operator.server.pub.main.OperatorService
import pl.com.stream.verto.crm.activity.server.pub.main.ActivityDto
import pl.com.stream.verto.crm.activity.server.pub.main.ActivityService
import pl.com.stream.verto.crm.activity.server.pub.type.ActivityTypeDto
import pl.com.stream.verto.crm.activity.server.pub.type.ActivityTypeService


class Informacja_o_koniecznosci_akceptacji extends pl.com.stream.next.asen.common.groovy.ServerScriptEnv {
    def script() {


        /*
         * 1. Pobranie podmiotu akceptacji.
         * 2. Sprawdzenie, która ścieżka akceptacji została wpisana do historii tabeli z historią 
         * 3. Jeżeli ścieżka została wpisania to należy dodać aktywoność dla operatorów wpisanych do podmiotu akceptacji.
         * 4. Aktywność musi mieć cechę z id_Podmiotu i id_ścieżki
         * 5. Przed wygenerowanie aktywności trzeba sprawdzić czy dana aktywość dla podanych użytkowników nie została już wygenrowana
         * 6. Generowanie aktywnośći polega na podaniu informacji dla osób przypiętych do kolejnego poziomu akceptacji
         * 7. Jeżeli nie ma wpisu to pierwsza aktywność 
         */


        Long idAcceptanceSubject = inParams.idAcceptanceSubject;

        Long idAcceptanceSubjectDef = 101605L; //Cecha aktywnosci
        Long idAcceptancePathLevelDef = 101604L; //Cecha aktywnosci
        Long idActivityType = 100300L; // Akceptacja podmiotu


        def ReportInfo = [
            "Podmiot zaakceptowany",
            "Wysłano już powiadominie",
            "Brak przypisanych operatorów do ścieżki akceptacji"
        ];

        def sqlReport =[
            "Select z.DOCUMENT_NUMBER from verto.PURCHASE_ORDER_DOCUMENT_V z where z.ID_ACCEPTANCE_SUBJECT =:idAcceptanceSubject",
            "Select z.DOCUMENT_NUMBER from verto.INTER_DISPATCH_ORDER_DOC_V z where z.ID_ACCEPTANCE_SUBJECT =:idAcceptanceSubject",
            "Select z.DOCUMENT_NUMBER from verto.SALE_ORDER_DOCUMENT_V z where z.ID_ACCEPTANCE_SUBJECT =:idAcceptanceSubject"
        ];

        Report report = requestContext.getReport();
        ReportLine rL = report.createLine();

        String sReport = "";

        report.setTitle( "Raport generatora powiadomień ");


        ActivityService  activityService = context.getService(ActivityService.class);
        AttributeValueService attributeValueService = context.getService(AttributeValueService.class);
        ActivityTypeService  activityTypeService = context.getService(ActivityTypeService.class);
        OperatorService  operatorService = context.getService(OperatorService.class);


        String sql = """select av.id_acceptance_path, av.is_accepted,al.lp,  av.id_acceptance_subject_source  
                     from verto.ACCEPTANCE_SUBJECT_V av
                     LEFT OUTER JOIN verto.ACCEPTANCE_PATH_LEVEL_V al ON (al.ID_ACCEPTANCE_PATH_LEVEL = av.ID_ACCEPTANCE_PATH_LEVEL)
                     where av.ID_ACCEPTANCE_SUBJECT =:idAcceptanceSubject""";

        List listResult = dm.executeSQLQuery(sql, ["idAcceptanceSubject":idAcceptanceSubject]);


        for (item in listResult) {

            int isAccepted = item[1];

            if (isAccepted == 0) {
                //Dla braku ścieżki należy pobrać pierwszą ze słownika.

                //Pobranie danych dotyczących aktywności

                ActivityTypeDto activityTypeDto = activityTypeService.find(idActivityType);

                int avetageTime = activityTypeDto.averageExecutionTime;

                Date planFrom = Date.getCurrentDateAndTime();
                Date planTo = planFrom.addMinute(avetageTime);

                int lp;

                if (item[2].equals(null)) {
                    lp = 0
                }
                else{
                    lp = item[2];
                }
                lp = lp + 1;


                sql ="Select al.id_acceptance_path_level, al.name from  verto.ACCEPTANCE_PATH_LEVEL_V al where al.ID_ACCEPTANCE_PATH =:idAcceptancePath and al.lp =:lp";
                List listPathResult = dm.executeSQLQuery(sql, ["idAcceptancePath" : item[0], "lp" : lp]);

                if (listPathResult.size() == 0) {
                    sReport = ReportInfo[0];
                }

                for (itemPath in listPathResult) {
                    //Wczytanie informacji o użytkownikach przyporządkowanych do ścieżki akceptacji

                    List<Long> idsOperator = new ArrayList<Long>();//Lista operatorów podpieta do grupy

                    sql = """
                            Select o.id_operator from  verto.accept_path_level_oper_grou_v a 
                            join assigned_operator_group_v o on o.id_operator_group = a.id_operator_group
                            where a.id_acceptance_path_level =:idAcceptancePathLevel and a.id_operator_group <> 1
                          """;

                    List listOperGroupResult = dm.executeSQLQuery(sql, ["idAcceptancePathLevel" : itemPath[0]]);

                    for ( Long itemOperGroup in listOperGroupResult) {
                        idsOperator.add(itemOperGroup);
                    }



                    sql = "Select a.id_operator from verto.acceptance_path_level_oper_v a where a.id_acceptance_path_level =:idAcceptancePathLevel";
                    List listOperResult = dm.executeSQLQuery(sql, ["idAcceptancePathLevel" : itemPath[0]]);

                    for ( Long itemOper in listOperResult) {
                        idsOperator.add(itemOper);
                    }

                    //Tylko unikalne wartości
                    Set<Long> uniqueIdsOperator = new HashSet<String>(idsOperator);

                    if (uniqueIdsOperator.size() == 0) {
                        sReport = ReportInfo[2];
                        //sReport ="Brak osób"
                    }

                    for (Long idOperator in uniqueIdsOperator) {
                        //Wyszukanie aktywnosci dla idOperator i wartosci cech idAcceptancePathLevel,idAcceptanceSubject

                        sql = """
                                Select count(*) from verto.activity_v a
                                join verto.attribute_value_v aPl on (aPl.id_attribute_subject = a.id_attribute_subject) and (aPl.id_attribute_definition =:idAcceptancePathLevelDef)
                                join verto.attribute_value_v aPS on (aPS.id_attribute_subject = a.id_attribute_subject) and (aPS.id_attribute_definition =:idAcceptanceSubjectDef)
                                where a.id_operator_lead =:idOperator and  aPl.numeric_value =:idAcceptancePathLevel and aPs.numeric_value =:idAcceptanceSubject
                                """;

                        List listActivityResult = dm.executeSQLQuery(sql, ["idAcceptancePathLevelDef" : idAcceptancePathLevelDef, "idAcceptanceSubjectDef" : idAcceptanceSubjectDef,
                            "idOperator" : idOperator,"idAcceptancePathLevel":itemPath[0],"idAcceptanceSubject":idAcceptanceSubject]);

                        OperatorDto operatorDto= operatorService.find(idOperator);

                        if (listActivityResult.size() > 0) {
                            //Dodanie nowej Aktywności

                            if (listActivityResult[0] == 0) {

                                String nrSql ="";
                                String nrDokInfo="Proszę o akceptację ";

                                switch(item[3]){
                                    case 100002 :
                                        nrSql = sqlReport[0]; //Zamówienia do dostawców
                                        nrDokInfo = nrDokInfo + " dokumentu zamówienia do dostawcy o numerze : ";
                                        break;
                                    case 100000 :
                                        nrSql = sqlReport[1]; //Zamówienia wewnętrzne
                                        nrDokInfo = nrDokInfo + " dokumentu zamówienia wewnętrznego o numerze : ";
                                        break;
                                    case 100004 :
                                        nrSql = sqlReport[2]; //Zamówienia od odbiorców
                                        nrDokInfo = nrDokInfo + " dokumentu zamówienia od odbiorcy o numerze : ";
                                        break;
                                    default:
                                        nrSql="";
                                }



                                if(nrSql != ""){
                                    List listDocumentAccept = dm.executeSQLQuery(nrSql, ["idAcceptanceSubject":idAcceptanceSubject]);
                                    nrDokInfo = nrDokInfo + listDocumentAccept[0];
                                }

                                ActivityDto activityDto = new ActivityDto();

                                activityDto.idActivityType = idActivityType;
                                activityDto.idOperatorOwner = idOperator;
                                activityDto.idOperatorLead = idOperator;

                                activityDto.planFrom = planFrom
                                activityDto.planTo = planTo;
                                activityDto.description = nrDokInfo;

                                activityDto = activityService.init(activityDto);
                                Long idActivity = activityService.insert(activityDto);

                                activityService.setState(idActivity, 1L);

                                activityDto = activityService.find(idActivity);

                                attributeValueService.insertAttributeValueBySubjectAndDef(activityDto.idAttributeSubject, idAcceptancePathLevelDef, new Decimal(itemPath[0]));
                                attributeValueService.insertAttributeValueBySubjectAndDef(activityDto.idAttributeSubject, idAcceptanceSubjectDef, new Decimal(idAcceptanceSubject));

                                sReport = sReport + "<p>Dodano powiadomienie dla operatora : " + operatorDto.firstName + " " + operatorDto.lastName + "</p>";
                            }
                            else{

                                sReport = sReport + "<p>" + ReportInfo[1] + " dla operatora : " + operatorDto.firstName + " " + operatorDto.lastName + "</p>";
                            }
                        }
                    }
                }
            }
            else{
                sReport = ReportInfo[0];
            }

        }

        rL.addText(sReport);

        report.println(rL);
    }
}