function appendToMsgArea(entries) {
    // add the relevant entries
    $.each(entries || [], appendMsgEntry);

    var scroller = $("#msgarea");
    var height = scroller.scrollTop() - $(scroller).height();
    $(scroller).stop().animate({ scrollTop: height }, "slow");
    messages = $("#msgarea")[0].innerHTML;
}

function appendMsgEntry(index, entry){
    var entryElement = createMsgEntry(entry);
    $("#msgarea").append(entryElement).append("<br>");
}

function createMsgEntry (entry){
    return $("<span class=\"success\">").append("<b>" + entry.timestamp + "> </b>" + entry.message);
}


$(function() { // onload...do
    //add a function to the submit event
    $("#msgform").submit(function() {
        $.ajax({
            data: $(this).serialize(),
            url: "getMessages",
            timeout: 2000,
            error: function() {
                console.error("Failed to submit");
            },
            success: function(r) {
                //since it's going to be retrieved from the client.server
                //$("#result h1").text(r);
            }
        });

        $("#messages").val("");
        // by default - we'll always return false so it doesn't redirect the user.
        return false;
    });
});

function triggerAjaxMsgContent() {
    setTimeout(ajaxMsgContent, refreshRate);
}
