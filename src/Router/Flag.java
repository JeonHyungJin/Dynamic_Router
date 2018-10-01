package Router;


/**
 * Flag 
 * U : 경로가 유효한 상태
 * G : 해당 경로가 Gateway(라우터)를 향하고 있는 상태
 * H : 해당 경로가 호스트를 향하고 있는 상태 
 * UG : 경로가 유효하고 라우터를 향하고 있는 상태
 * UH : 경로가 유효하고 호스트를 향하고 있는 상태
 * 
 * */
public enum Flag {
   U, G, H, UG, UH, NONE
}