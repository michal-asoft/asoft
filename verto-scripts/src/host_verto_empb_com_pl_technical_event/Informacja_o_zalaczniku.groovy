package host_verto_empb_com_pl_technical_event;

import pl.com.stream.verto.cmm.attachement.server.pub.main.AttachementDto
import pl.com.stream.verto.cmm.attachement.server.pub.main.AttachementService
import pl.com.stream.verto.cmm.attribute.server.pub.value.AttributeValueService

class Informacja_o_zalaczniku extends pl.com.stream.next.asen.common.groovy.TechnicalEventScriptEnv {
    def script() {

        Long idAttributeDef = 101102L;

        List<String> hqlList =
                [
                    "Select s.attributeSubject.idAttributeSubject from SaleOrderDocument s where s.attachementSubject.idAttachementSubject =:idAttachementSubject",
                    "Select s.attributeSubject.idAttributeSubject from SaleDocument s where s.attachementSubject.idAttachementSubject =:idAttachementSubject",
                    "Select s.attributeSubject.idAttributeSubject from SaleRequestDocument s where s.attachementSubject.idAttachementSubject =:idAttachementSubject",
                    "Select s.attributeSubject.idAttributeSubject from PurchaseOrderDocument s where s.attachementSubject.idAttachementSubject =:idAttachementSubject"
                ];

        Long idAttachement = event.methodResult.getResult();

        AttachementService attachementService = context.getService(AttachementService.class);
        AttributeValueService attributeValueService = context.getService(AttributeValueService.class);

        AttachementDto attachementDto = attachementService.find(idAttachement);
        def idAttachementSubject = attachementDto.idAttachementSubject;

        //throw new BusinessOperationException(idAttachementSubject.toString());


        for (hql in hqlList) {
            List<Long> ids = dm.executeQuery(hql, ["idAttachementSubject":idAttachementSubject]);


            if (ids.size()>0) {
                Long idAttributeSubject = ids.get(0);
                attributeValueService.insertAttributeValueBySubjectAndDef(idAttributeSubject, idAttributeDef, true);
            }
        }




        //
    }
}